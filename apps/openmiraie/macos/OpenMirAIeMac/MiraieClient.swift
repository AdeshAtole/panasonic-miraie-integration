import Foundation
import Security

struct MiraieSession: Codable {
    let accessToken: String
    let refreshToken: String
}

protocol SessionStorage {
    func load() throws -> MiraieSession?
    func save(_ session: MiraieSession) throws
    func clear() throws
}

final class KeychainSessionStore: SessionStorage {
    private let query: [String: Any]
    init(service: String = "in.openmiraie.macos.session") {
        query = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: service,
                 kSecAttrAccount as String: "MirAIe India", kSecAttrSynchronizable as String: false]
    }
    func load() throws -> MiraieSession? {
        var q = query
        q[kSecReturnData as String] = true
        q[kSecMatchLimit as String] = kSecMatchLimitOne
        var result: CFTypeRef?
        let status = SecItemCopyMatching(q as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess, let data = result as? Data,
              let session = try? JSONDecoder().decode(MiraieSession.self, from: data), !session.accessToken.isEmpty else { throw MiraieError.secureStorage }
        return session
    }
    func save(_ session: MiraieSession) throws {
        let data = try JSONEncoder().encode(session)
        let updates = [kSecValueData as String: data]
        let status = SecItemUpdate(query as CFDictionary, updates as CFDictionary)
        if status == errSecSuccess { return }
        guard status == errSecItemNotFound else { throw MiraieError.secureStorage }
        var q = query
        q[kSecValueData as String] = data
        q[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        guard SecItemAdd(q as CFDictionary, nil) == errSecSuccess else { throw MiraieError.secureStorage }
    }
    func clear() throws {
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else { throw MiraieError.secureStorage }
    }
}

@MainActor
protocol MiraieTransport {
    func send(_ request: URLRequest) async throws -> (Data, Int)
}

private final class RefuseRedirects: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse,
                    newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
        completionHandler(nil)
    }
}

@MainActor
final class CloudTransport: MiraieTransport {
    private let session: URLSession
    init() {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 15
        configuration.timeoutIntervalForResource = 25
        configuration.urlCache = nil
        configuration.httpCookieStorage = nil
        configuration.httpShouldSetCookies = false
        session = URLSession(configuration: configuration, delegate: RefuseRedirects(), delegateQueue: nil)
    }
    func send(_ request: URLRequest) async throws -> (Data, Int) {
        guard ProcessInfo.processInfo.environment["OPENMIRAIE_NO_NETWORK"] != "1" else {
            throw MiraieError.blocked("Network access is disabled during automated tests.")
        }
        let (data, response) = try await session.data(for: request)
        guard let response = response as? HTTPURLResponse, data.count <= 4 * 1024 * 1024 else { throw MiraieError.malformed }
        return (data, response.statusCode)
    }
}

@MainActor
final class MiraieClient {
    static let authBase = "https://auth.miraie.in/simplifi/v1/"
    static let appBase = "https://app.miraie.in/simplifi/v1/"
    static let clientID = "292d99d7-5623-4157-8a71-846ee3ff5dbf"
    private let storage: SessionStorage
    private let transport: MiraieTransport
    private var session: MiraieSession?
    private var refreshTask: Task<MiraieSession, Error>?
    private(set) var revision = UUID()
    var isSignedIn: Bool { session != nil }

    init(storage: SessionStorage, transport: MiraieTransport) throws {
        self.storage = storage
        self.transport = transport
        session = try storage.load()
    }
    static func segment(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .alphanumerics.union(CharacterSet(charactersIn: "-._~"))) ?? ""
    }
    private func perform(base: String, path: String, method: String = "GET", body: [String: Any]? = nil, token: String? = nil) async throws -> Any {
        guard let url = URL(string: base + path), url.scheme == "https" else { throw MiraieError.malformed }
        var request = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 15)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token { request.setValue("Bearer " + token, forHTTPHeaderField: "Authorization") }
        if let body { request.httpBody = try JSONSerialization.data(withJSONObject: body) }
        let (data, status) = try await transport.send(request)
        guard (200..<300).contains(status) else { throw MiraieError.http(status) }
        if data.isEmpty { return [String: Any]() }
        guard let json = try? JSONSerialization.jsonObject(with: data) else { throw MiraieError.malformed }
        return json
    }
    private func save(_ session: MiraieSession) throws {
        try storage.save(session)
        self.session = session
        revision = UUID()
    }
    func signIn(identity: String, password: String) async throws {
        let identity = identity.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !identity.isEmpty, !password.isEmpty else { throw MiraieError.blocked("Enter your MirAIe email or mobile number and password.") }
        let json = try await perform(base: Self.authBase, path: "userManagement/login", method: "POST", body: [
            identity.contains("@") ? "email" : "mobile": identity, "password": password,
            "clientId": Self.clientID, "scope": "an_" + UUID().uuidString
        ])
        guard let response = json as? [String: Any], let access = response["accessToken"] as? String, !access.isEmpty else { throw MiraieError.malformed }
        try save(MiraieSession(accessToken: access, refreshToken: response["refreshToken"] as? String ?? ""))
    }
    func signOut() throws {
        refreshTask?.cancel()
        refreshTask = nil
        session = nil
        revision = UUID()
        try storage.clear()
    }
    private func refresh(_ original: MiraieSession) async throws -> MiraieSession {
        if let refreshTask { return try await refreshTask.value }
        guard !original.refreshToken.isEmpty else { throw MiraieError.signedOut }
        let revision = revision
        let task = Task<MiraieSession, Error> { [self] in
            let json = try await perform(base: Self.authBase, path: "userManagement/tokenRefresh", method: "POST",
                                         body: ["clientId": Self.clientID, "token": original.refreshToken])
            guard let response = json as? [String: Any], let access = response["accessToken"] as? String, !access.isEmpty else { throw MiraieError.signedOut }
            let nextRefresh = response["refreshToken"] as? String ?? ""
            let result = MiraieSession(accessToken: access, refreshToken: nextRefresh.isEmpty ? original.refreshToken : nextRefresh)
            guard self.revision == revision, !Task.isCancelled else { throw MiraieError.signedOut }
            try save(result)
            return result
        }
        refreshTask = task
        defer { refreshTask = nil }
        return try await task.value
    }
    func authorized(path: String, method: String = "GET", body: [String: Any]? = nil) async throws -> Any {
        guard let original = session else { throw MiraieError.signedOut }
        do { return try await perform(base: Self.appBase, path: path, method: method, body: body, token: original.accessToken) }
        catch MiraieError.http(let code) where code == 401 || code == 406 {
            // Never replay a mutation, even after an authorization response.
            guard method == "GET" else { try? signOut(); throw MiraieError.signedOut }
            do {
                let current: MiraieSession
                if let saved = session, saved.accessToken != original.accessToken { current = saved }
                else { current = try await refresh(original) }
                return try await perform(base: Self.appBase, path: path, token: current.accessToken)
            } catch MiraieError.http(let code) where code == 401 || code == 406 {
                try? signOut(); throw MiraieError.signedOut
            } catch MiraieError.signedOut {
                try? signOut(); throw MiraieError.signedOut
            }
        }
    }
    func devices() async throws -> [MiraieDevice] { try MiraieDevice.discover(await authorized(path: "homeManagement/homes")) }
    func profile(_ device: MiraieDevice) async throws -> ACProfile {
        guard !device.modelID.isEmpty else { throw MiraieError.blocked("The AC model was not reported.") }
        return try ACProfile(await authorized(path: "deviceManagement/deviceModels/modelId/" + Self.segment(device.modelID)), device: device)
    }
    func status(_ device: MiraieDevice) async throws -> ACStatus {
        try ACStatus(await authorized(path: "deviceManagement/devices/" + Self.segment(device.id) + "/mobile/status"))
    }
    func send(_ command: ControlCommand, device: MiraieDevice) async throws {
        _ = try await authorized(path: "deviceManagement/control", method: "POST", body: ["deviceId": device.id, "control_params": command.parameters])
    }
}

struct CommandOutcome {
    let status: ACStatus
    let confirmed: Int
    let total: Int
    let message: String
    var complete: Bool { confirmed == total }
}

@MainActor
final class CommandExecutor {
    let client: MiraieClient
    let wait: (Double) async throws -> Void
    init(client: MiraieClient, wait: @escaping (Double) async throws -> Void = { seconds in try await Task.sleep(for: .seconds(seconds)) }) {
        self.client = client
        self.wait = wait
    }
    func execute(action: ControlAction, value: ControlValue, device: MiraieDevice,
                 observed: (ACProfile, ACStatus) -> Void) async throws -> CommandOutcome {
        // Fresh model/state are read before constructing any mutation.
        let profile = try await client.profile(device)
        var status = try await client.status(device)
        try Task.checkCancellation()
        observed(profile, status)
        let plan = try profile.plan(action, value: value, status: status)
        if plan.allSatisfy({ Wire.same(status.fields[$0.key], $0.value.wire) }) {
            return CommandOutcome(status: status, confirmed: plan.count, total: plan.count, message: "The AC already reports this setting.")
        }
        let revision = client.revision
        let initialMode = profile.value(.mode, status)
        var confirmed = 0
        for step in plan {
            try Task.checkCancellation()
            guard revision == client.revision else {
                return CommandOutcome(status: status, confirmed: confirmed, total: plan.count, message: "Session changed. Remaining changes were stopped. Refresh before trying again.")
            }
            if confirmed > 0 && profile.value(.mode, status) != initialMode {
                return CommandOutcome(status: status, confirmed: confirmed, total: plan.count, message: "The AC mode changed. Remaining changes were stopped.")
            }
            // Revalidate each dependent step against the latest actual report.
            do { _ = try profile.command(step.action, value: step.value, status: status) }
            catch { return CommandOutcome(status: status, confirmed: confirmed, total: plan.count, message: "Stopped after \(confirmed) confirmed changes. " + MiraieError.safe(error)) }
            if Wire.same(status.fields[step.key], step.value.wire) { confirmed += 1; continue }
            do { try await client.send(step, device: device) }
            catch {
                return CommandOutcome(status: status, confirmed: confirmed, total: plan.count,
                                      message: "Delivery is unconfirmed. Refresh to check the AC. " + MiraieError.safe(error))
            }
            var matching = false
            for delay in [3.0, 1.2, 1.8] {
                do {
                    try await wait(delay)
                    try Task.checkCancellation()
                    status = try await client.status(device)
                    observed(profile, status)
                    guard status.online == true else { break }
                    if Wire.same(status.fields[step.key], step.value.wire) { matching = true; break }
                } catch {
                    return CommandOutcome(status: status, confirmed: confirmed, total: plan.count,
                                          message: "Readback is unconfirmed. Refresh to check the AC. " + MiraieError.safe(error))
                }
            }
            guard matching else {
                return CommandOutcome(status: status, confirmed: confirmed, total: plan.count,
                                      message: "The AC has not reported the change. Refresh to check. \(confirmed) of \(plan.count) changes confirmed.")
            }
            confirmed += 1
        }
        return CommandOutcome(status: status, confirmed: confirmed, total: plan.count, message: "AC reports the updated setting.")
    }
}
