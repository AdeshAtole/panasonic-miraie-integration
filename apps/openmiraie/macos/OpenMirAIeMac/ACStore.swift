import SwiftUI

@MainActor
final class ACStore: ObservableObject {
    @Published private(set) var signedIn = false
    @Published private(set) var loading = false
    @Published private(set) var devices: [MiraieDevice] = []
    @Published private(set) var selected: MiraieDevice?
    @Published private(set) var profile: ACProfile?
    @Published private(set) var reported: ACStatus?
    @Published private(set) var pending: String?
    @Published private(set) var requestedTarget: Double?
    @Published private(set) var unconfirmed = false
    @Published private(set) var stale = true
    @Published private(set) var feedback = ""
    @Published private(set) var signInError = ""
    private var client: MiraieClient?
    private var task: Task<Void, Never>?
    private var generation = UUID()
    var isPreview = false
    var panelVisible = false
    private var active = true
    private let preferences: UserDefaults

    init(client injected: MiraieClient? = nil, preferences: UserDefaults = .standard, restoreSession: Bool = true) {
        self.preferences = preferences
        if let injected { client = injected; signedIn = injected.isSignedIn; return }
        guard restoreSession else { return }
        do {
            let api = try MiraieClient(storage: KeychainSessionStore(), transport: CloudTransport())
            client = api
            signedIn = api.isSignedIn
        } catch { signInError = MiraieError.safe(error) }
    }
    var acDevices: [MiraieDevice] { devices.filter { $0.category.uppercased() == "AC" } }
    var title: String { selected?.title ?? "Choose an Air Conditioner" }
    var power: Bool? { reported.flatMap { profile?.isOn(.power, $0) } }
    var display: Bool? { reported.flatMap { profile?.isOn(.display, $0) } }
    var eco: Bool? { reported.flatMap { profile?.isOn(.eco, $0) } }
    var powerful: Bool? { reported.flatMap { profile?.isOn(.powerful, $0) } }
    var target: Double? { Wire.number(value(.temperature)) }
    var mode: String? { value(.mode)?.lowercased() }
    var temperatureBounds: TemperatureBounds? { mode.flatMap { profile?.bounds(mode: $0) } }
    var online: Bool? { reported?.online }
    var cleaning: Bool? { reported.flatMap { profile?.cleaning($0) } }
    var isStale: Bool { stale || reported.map { Date().timeIntervalSince($0.observedAt) > 90 } ?? true }
    var menuTitle: String {
        guard !isStale, !unconfirmed, pending == nil, online == true else { return "" }
        if power == false { return "Off" }
        guard power == true, mode != "fan", let target else { return "" }
        return target.formatted(.number.precision(.fractionLength(0...1))) + "°"
    }
    var status: String {
        if pending != nil { return "Applying Change…" }
        if unconfirmed { return "Change Unconfirmed" }
        if isStale { return reported == nil ? (loading ? "Loading Status…" : "Status Unavailable") : "Last Known State" }
        if online != true { return online == false ? "Offline" : "Connection Unknown" }
        if cleaning == true { return "Cleaning" }
        if power == false { return "Off" }
        if power == true { return "On · " + label(mode) }
        return "Power Not Reported"
    }
    func value(_ action: ControlAction) -> String? { reported.flatMap { profile?.value(action, $0) } }
    func choices(_ action: ControlAction) -> [String] { profile?.choices(action) ?? [] }
    func positions(_ action: ControlAction) -> [Int] { profile?.positions(action) ?? [] }
    func label(_ value: String?) -> String {
        guard let value, !value.isEmpty else { return "Not Reported" }
        return value.lowercased() == "fan" ? "Fan Only" : value.capitalized
    }
    func capacityLabel(_ value: String?) -> String {
        guard let value, let number = Double(value) else { return "Not Reported" }
        return number == 0 ? "Normal" : number.formatted(.number.precision(.fractionLength(0))) + "%"
    }
    func positionLabel(_ value: String?) -> String {
        guard let code = Wire.integer(value) else { return "Not Reported" }
        return code == 0 ? "Swing" : "Position \(code)"
    }
    func can(_ action: ControlAction) -> Bool {
        guard !loading, pending == nil, !unconfirmed, !isStale, active,
              let profile, let reported else { return false }
        return profile.blocked(action, status: reported) == nil
    }

    func bootstrap() async {
        guard signedIn, !loading else { return }
        await discover()
    }
    func signIn(identity: String, password: String) async {
        guard !loading, !signedIn else { return }
        let revision = generation
        loading = true
        signInError = ""
        defer { loading = false }
        do {
            if client == nil { client = try MiraieClient(storage: KeychainSessionStore(), transport: CloudTransport()) }
            guard let client else { throw MiraieError.secureStorage }
            try await client.signIn(identity: identity, password: password)
            guard generation == revision else { try? client.signOut(); return }
            signedIn = true
        } catch { signInError = MiraieError.safe(error); return }
        // Discovery only; signing in never sends an appliance command.
        loading = false
        await discover()
    }
    func discover() async {
        guard let client, !loading else { return }
        let revision = generation
        loading = true
        feedback = "Finding Air Conditioners…"
        defer { if generation == revision { loading = false } }
        do {
            let found = try await client.devices()
            guard generation == revision, !Task.isCancelled else { return }
            devices = found
            let savedID = preferences.string(forKey: "selectedDevice")
            if let existing = found.first(where: { $0.id == (selected?.id ?? savedID) && $0.category.uppercased() == "AC" }) {
                loading = false
                await choose(existing)
            } else {
                selected = nil; profile = nil; reported = nil; stale = true
                preferences.removeObject(forKey: "selectedDevice")
                feedback = acDevices.isEmpty ? "No air conditioners found in this MirAIe India account." : "Choose an air conditioner."
            }
        } catch { handle(error) }
    }
    func choose(_ device: MiraieDevice) async {
        guard pending == nil, !loading, let client, devices.contains(where: { $0.id == device.id }) else { return }
        task?.cancel()
        generation = UUID()
        let revision = generation
        // Clear the old room before presenting a newly selected room.
        if selected?.id != device.id { profile = nil; reported = nil }
        selected = device; stale = true; unconfirmed = false
        loading = true; feedback = "Loading AC…"
        defer { if generation == revision { loading = false } }
        do {
            let metadata = try await client.profile(device)
            let status = try await client.status(device)
            guard generation == revision, !Task.isCancelled else { return }
            profile = metadata; reported = status; stale = false
            feedback = metadata.supported ? "" : "Controls are unavailable for this model."
            preferences.set(device.id, forKey: "selectedDevice")
        } catch { if generation == revision { handle(error) } }
    }
    func refresh() async {
        guard pending == nil, !loading else { return }
        guard let device = selected else { await discover(); return }
        // Reload model and status; this never resends an earlier command.
        await choose(device)
    }
    func request(_ action: ControlAction, _ value: ControlValue) {
        guard can(action), let client, let device = selected else { return }
        let revision = generation
        pending = "Applying Change…"
        feedback = "Checking AC…"
        requestedTarget = action == .temperature ? Wire.number(value.wire) : nil
        task = Task { [weak self] in
            guard let self else { return }
            do {
                let outcome = try await CommandExecutor(client: client).execute(action: action, value: value, device: device) { profile, status in
                    if self.generation == revision { self.profile = profile; self.reported = status; self.stale = false }
                }
                guard self.generation == revision, !Task.isCancelled else { return }
                self.reported = outcome.status
                self.unconfirmed = !outcome.complete
                self.feedback = outcome.message
                if !client.isSignedIn { self.signedIn = false }
            } catch {
                guard self.generation == revision, !Task.isCancelled else { return }
                self.handle(error)
            }
            self.pending = nil
            self.requestedTarget = nil
        }
    }
    func markStale() { stale = true }
    func expireOldReport() { if isStale && !stale { stale = true } }
    func setActive(_ value: Bool) {
        active = value
        if !value, pending != nil {
            task?.cancel()
            generation = UUID()
            pending = nil; requestedTarget = nil; unconfirmed = true; stale = true
            feedback = "Change interrupted. Refresh to check the AC."
        }
    }
    func signOut() {
        task?.cancel(); generation = UUID()
        do { try client?.signOut() }
        catch { signInError = MiraieError.safe(error) }
        signedIn = false; devices = []; selected = nil; profile = nil; reported = nil
        pending = nil; requestedTarget = nil; unconfirmed = false; stale = true; feedback = ""
        preferences.removeObject(forKey: "selectedDevice")
    }
    private func handle(_ error: Error) {
        feedback = MiraieError.safe(error)
        stale = true
        if let client, !client.isSignedIn { signedIn = false; signInError = feedback }
    }
}

#if DEBUG
/// Explicit synthetic review mode: no Keychain, account, or network access.
private final class PreviewSession: SessionStorage {
    func load() throws -> MiraieSession? { MiraieSession(accessToken: "synthetic", refreshToken: "") }
    func save(_ session: MiraieSession) throws { }
    func clear() throws { }
}
@MainActor
private final class PreviewTransport: MiraieTransport {
    var fields: [String: Any] = ["ps":"on", "acmd":"cool", "actmp":"24.0", "rmtmp":"26.3", "acfs":"auto", "acdc":"on", "acem":"off", "acpm":"off", "cnv":0, "acvs":0, "achs":1, "acec":"off", "onlineStatus":true]
    static var model: [String: Any] {
        let mapping: [(ControlAction, String)] = [(.power,"ps"),(.mode,"acmd"),(.temperature,"actmp"),(.fan,"acfs"),(.display,"acdc"),(.eco,"acem"),(.powerful,"acpm"),(.capacity,"cnv"),(.vertical,"acvs"),(.horizontal,"achs")]
        let caps = mapping.map { action, key -> [String: Any] in
            var attr: [String: Any] = ["key":key]
            switch action {
            case .mode: attr["values"] = ["auto","cool","dry","fan"]
            case .fan: attr["values"] = ["auto","quiet","low","medium","high"]
            case .display,.eco: attr["values"] = ["on","off"]
            case .capacity: attr["intValues"] = [0,40,50,60,70,80,90,100,110]
            case .temperature:
                attr["range"] = ["min":[["unit":"celsius","value":16]],"max":[["unit":"celsius","value":30]],"increment":[["unit":"celsius","value":1]]]
            case .horizontal,.vertical:
                attr["range"] = ["min":[["value":0]],"max":[["value":5]],"increment":[["value":1]]]
            default: break
            }
            return ["capability":action.capability,"attributes":[attr]]
        }
        return ["modelId":"140013","controlCapabilities":caps]
    }
    func send(_ request: URLRequest) async throws -> (Data, Int) {
        let path = request.url?.path ?? ""
        if path.contains("deviceModels") { return (try JSONSerialization.data(withJSONObject: Self.model),200) }
        if path.hasSuffix("/mobile/status") { return (try JSONSerialization.data(withJSONObject: fields),200) }
        if path.hasSuffix("deviceManagement/control") {
            let body = try JSONSerialization.jsonObject(with: request.httpBody ?? Data()) as? [String:Any]
            for (key,value) in body?["control_params"] as? [String:Any] ?? [:] where !["cnt","sid","ki"].contains(key) { fields[key] = value }
            return (Data(),200)
        }
        throw MiraieError.blocked("Synthetic preview has no cloud connection.")
    }
}
extension ACStore {
    static func preview(state: String = "ready") -> ACStore {
        let cloud = PreviewTransport()
        let store = ACStore(client: try? MiraieClient(storage: PreviewSession(), transport: cloud), restoreSession: false)
        store.isPreview = true
        guard state != "signed-out" else { store.signedIn = false; return store }
        let device = MiraieDevice(id: "synthetic-ac", name: "Room AC", home: "Preview Home", space: "Living room", modelID: "140013", category: "AC", routed: false)
        if state == "offline" { cloud.fields["onlineStatus"] = false }
        if state == "off" { cloud.fields["ps"] = "off" }
        if state == "fan" { cloud.fields["acmd"] = "fan" }
        if state == "dry" { cloud.fields["acmd"] = "dry" }
        if state == "cleaning" { cloud.fields["acec"] = "on" }
        store.devices = [device]; store.selected = device
        store.profile = try? ACProfile(PreviewTransport.model, device: device)
        store.reported = try? ACStatus(cloud.fields)
        store.stale = false; store.panelVisible = true
        if state == "unconfirmed" { store.unconfirmed = true; store.feedback = "The AC has not reported the change. Refresh to check." }
        if state == "pending" { store.pending = "Applying Change…"; store.requestedTarget = 25 }
        return store
    }
}
#endif
