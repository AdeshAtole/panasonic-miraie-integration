import Foundation
import CoreFoundation

enum MiraieError: Error, LocalizedError {
    case signedOut, malformed, secureStorage, network, timeout, http(Int), blocked(String)
    var errorDescription: String? {
        switch self {
        case .signedOut: return "Your MirAIe session has expired. Sign in again. No change will be replayed."
        case .malformed: return "MirAIe returned an unexpected response. Refresh before changing settings."
        case .secureStorage: return "The secure session store is unavailable. Please try again after unlocking the device."
        case .network: return "Could not reach MirAIe. Check your internet connection and refresh."
        case .timeout: return "The request timed out. Refresh to check the AC before trying another change."
        case .http(let code): return "MirAIe returned HTTP \(code). Refresh before trying again."
        case .blocked(let reason): return reason
        }
    }
    static func safe(_ error: Error) -> String {
        if let known = error as? MiraieError { return known.localizedDescription }
        if let url = error as? URLError, url.code == .timedOut { return MiraieError.timeout.localizedDescription }
        if error is CancellationError { return "Change interrupted. Refresh to check the AC." }
        return MiraieError.network.localizedDescription
    }
}

enum Wire {
    static func text(_ value: Any?) -> String? {
        if let value = value as? String { return value }
        if let value = value as? NSNumber { return value.stringValue }
        return nil
    }
    static func number(_ value: Any?) -> Double? {
        if let n = value as? NSNumber, CFGetTypeID(n) == CFBooleanGetTypeID() { return nil }
        guard let text = text(value), let number = Double(text), number.isFinite else { return nil }
        return number
    }
    static func integer(_ value: Any?) -> Int? {
        guard let n = number(value), n.rounded() == n, n >= Double(Int.min), n < Double(Int.max) else { return nil }
        return Int(n)
    }
    static func present(_ value: Any?) -> Bool {
        guard let value, !(value is NSNull) else { return false }
        if let s = value as? String { return !s.isEmpty }
        if let n = value as? NSNumber { return n.boolValue }
        return true
    }
    static func same(_ a: Any?, _ b: Any?) -> Bool {
        guard let a, let b, !(a is NSNull), !(b is NSNull) else { return false }
        let aBoolean = (a as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } ?? false
        let bBoolean = (b as? NSNumber).map { CFGetTypeID($0) == CFBooleanGetTypeID() } ?? false
        if aBoolean || bBoolean { return aBoolean && bBoolean && (a as? NSNumber)?.boolValue == (b as? NSNumber)?.boolValue }
        if let a = number(a), let b = number(b) { return abs(a - b) < 0.000001 }
        guard let a = text(a), let b = text(b) else { return false }
        return a.caseInsensitiveCompare(b) == .orderedSame
    }
}

struct MiraieDevice: Identifiable {
    let id: String
    let name: String
    let home: String
    let space: String
    let modelID: String
    let category: String
    let routed: Bool
    var title: String { space.isEmpty ? name : space }
    var supportsReviewedAdapter: Bool { category.uppercased() == "AC" && modelID == "140013" && !routed }

    static func discover(_ json: Any) throws -> [MiraieDevice] {
        guard let homes = json as? [[String: Any]] else { throw MiraieError.malformed }
        var devices: [MiraieDevice] = []
        var seen = Set<String>()
        for home in homes {
            for space in home["spaces"] as? [[String: Any]] ?? [] {
                for d in space["devices"] as? [[String: Any]] ?? [] {
                    guard let id = d["deviceId"] as? String, !id.isEmpty, seen.insert(id).inserted else { continue }
                    devices.append(MiraieDevice(id: id, name: d["deviceName"] as? String ?? "Air Conditioner",
                        home: home["homeName"] as? String ?? "Home", space: space["spaceName"] as? String ?? "",
                        modelID: Wire.text(d["modelId"]) ?? "", category: d["category"] as? String ?? "",
                        routed: Wire.present(d["parentDeviceId"]) || Wire.present(d["thirdPartyService"])))
                }
            }
        }
        return devices
    }
}

enum ControlAction: String, CaseIterable {
    case power, mode, temperature, fan, display, eco, powerful, capacity, vertical, horizontal
    var capability: String {
        switch self {
        case .power: return "PowerStatus"
        case .mode: return "ACMode"
        case .temperature: return "ACTemperature"
        case .fan: return "ACFanSpeed"
        case .display: return "DisplayControl"
        case .eco: return "ECOMode"
        case .powerful: return "PowerfulMode"
        case .capacity: return "Convertible"
        case .vertical: return "VerticalSwing"
        case .horizontal: return "HorizontalSwing"
        }
    }
    var index: Int {
        switch self {
        case .mode: return 0
        case .power: return 1
        case .temperature: return 2
        case .powerful: return 3
        case .fan: return 4
        case .vertical: return 5
        case .horizontal: return 6
        case .display: return 8
        case .eco: return 10
        case .capacity: return 28
        }
    }
}

enum ControlValue: Equatable {
    case text(String), number(Double), integer(Int)
    var wire: Any {
        switch self {
        case .text(let v): return v
        case .number(let v): return String(v)
        case .integer(let v): return v
        }
    }
    static func toggle(_ on: Bool) -> ControlValue { .text(on ? "on" : "off") }
}

struct ControlCommand {
    let action: ControlAction
    let key: String
    let value: ControlValue
    var parameters: [String: Any] { ["cnt": "an", "sid": "0", "ki": action.index, key: value.wire] }
}

struct TemperatureBounds: Equatable {
    let minimum: Double
    let maximum: Double
    let step: Double
    func contains(_ value: Double) -> Bool {
        value.isFinite && value >= minimum && value <= maximum && abs((value - minimum) / step - ((value - minimum) / step).rounded()) < 0.000001
    }
}

struct ACStatus {
    let fields: [String: Any]
    let observedAt: Date
    init(_ json: Any, at date: Date = Date()) throws {
        guard var fields = json as? [String: Any] else { throw MiraieError.malformed }
        for _ in 0..<4 {
            guard fields.count == 1, let key = fields.keys.first, ["status", "data", "params", "state"].contains(key) else { break }
            guard let next = fields[key] as? [String: Any] else { throw MiraieError.malformed }
            fields = next
        }
        guard !Set(fields.keys).isDisjoint(with: ["ps", "acmd", "actmp", "acfs", "acdc", "acem", "acpm", "cnv", "rmtmp", "onlineStatus", "acec", "acvs", "achs"]) else { throw MiraieError.malformed }
        self.fields = fields
        observedAt = date
    }
    var online: Bool? {
        if let s = fields["onlineStatus"] as? String {
            if ["online", "true"].contains(s.lowercased()) { return true }
            if ["offline", "false"].contains(s.lowercased()) { return false }
            return nil
        }
        if let n = fields["onlineStatus"] as? NSNumber {
            if n.doubleValue == 1 { return true }
            if n.doubleValue == 0 { return false }
        }
        return nil
    }
    var roomTemperature: Double? {
        guard let value = Wire.number(fields["rmtmp"]), value != -1 else { return nil }
        return value
    }
}

struct ACProfile {
    let supported: Bool
    private let attributes: [String: [String: Any]]
    init(_ json: Any, device: MiraieDevice) throws {
        guard var model = json as? [String: Any] else { throw MiraieError.malformed }
        if model["controlCapabilities"] == nil, let wrapped = model["data"] as? [String: Any] { model = wrapped }
        supported = device.supportsReviewedAdapter && !Wire.present(model["thirdPartyService"])
            && (Wire.text(model["modelId"]) ?? device.modelID) == device.modelID
        var attributes: [String: [String: Any]] = [:]
        var seen = Set<String>()
        for cap in model["controlCapabilities"] as? [[String: Any]] ?? [] {
            guard let name = cap["capability"] as? String else { continue }
            guard seen.insert(name).inserted else { attributes.removeValue(forKey: name); continue }
            guard let attrs = cap["attributes"] as? [[String: Any]], attrs.count == 1,
                  let attr = attrs.first, let key = attr["key"] as? String, !key.isEmpty,
                  !["cnt", "sid", "ki", "deviceId"].contains(key) else { continue }
            attributes[name] = attr
        }
        self.attributes = attributes
    }
    func key(_ action: ControlAction) -> String? { supported ? attributes[action.capability]?["key"] as? String : nil }
    func value(_ action: ControlAction, _ status: ACStatus) -> String? {
        guard let key = key(action) else { return nil }
        return Wire.text(status.fields[key])
    }
    func isOn(_ action: ControlAction, _ status: ACStatus) -> Bool? {
        guard let value = value(action, status)?.lowercased(), ["on", "off"].contains(value) else { return nil }
        return value == "on"
    }
    func choices(_ action: ControlAction) -> [String] {
        guard key(action) != nil else { return [] }
        let allowed: [String]
        switch action {
        case .mode: allowed = ["auto", "cool", "dry", "fan"]
        case .fan: allowed = ["auto", "quiet", "low", "medium", "high"]
        case .capacity:
            let values = attributes[action.capability]?["intValues"] as? [Any] ?? []
            return [0,40,50,60,70,80,90,100,110].filter { candidate in values.contains { Wire.integer($0) == candidate } }.map(String.init)
        default: allowed = ["on", "off"]
        }
        let attr = attributes[action.capability] ?? [:]
        // Reviewed model 140013 has no enum on PowerStatus or PowerfulMode.
        if attr["values"] == nil && [.power, .powerful].contains(action) { return allowed }
        guard let values = attr["values"] as? [String] else { return [] }
        return allowed.filter { expected in values.contains { $0.lowercased() == expected } }
    }
    func bounds(mode: String) -> TemperatureBounds? {
        guard key(.temperature) != nil, let value = attributes["ACTemperature"]?["range"] else { return nil }
        let ranges = (value as? [[String: Any]]) ?? (value as? [String: Any]).map { [$0] } ?? []
        let matches = ranges.filter { ($0["mode"] as? String).map { $0.lowercased() == mode.lowercased() } ?? true }
        guard matches.count == 1, let range = matches.first else { return nil }
        func celsius(_ name: String) -> Double? {
            let entries = (range[name] as? [[String: Any]] ?? []).filter { ["celsius", "celcius", "c", "°c"].contains(($0["unit"] as? String ?? "").lowercased()) }
            return entries.count == 1 ? Wire.number(entries[0]["value"]) : nil
        }
        guard let low = celsius("min"), let high = celsius("max"), let step = celsius("increment"),
              low >= 16, high <= 30, low <= high, step > 0 else { return nil }
        return TemperatureBounds(minimum: low, maximum: high, step: step)
    }
    func positions(_ action: ControlAction) -> [Int] {
        guard [.vertical,.horizontal].contains(action), key(action) != nil, let attr = attributes[action.capability] else { return [] }
        var positions: [Int]?
        if let raw = attr["range"] {
            let ranges = (raw as? [[String: Any]]) ?? (raw as? [String: Any]).map { [$0] } ?? []
            guard ranges.count == 1, let range = ranges.first, range["mode"] == nil else { return [] }
            func bound(_ name: String) -> Int? {
                guard let entries = range[name] as? [[String: Any]], entries.count == 1,
                      !Wire.present(entries[0]["unit"]) else { return nil }
                return Wire.integer(entries[0]["value"])
            }
            guard let low = bound("min"), let high = bound("max"), let step = range["increment"] == nil ? 1 : bound("increment"),
                  0 <= low, low <= high, high <= 5, step >= 1 else { return [] }
            positions = Array(stride(from: low, through: high, by: step))
        }
        if let raw = attr["intValues"] {
            guard let values = raw as? [Any], !values.isEmpty, values.count <= 6 else { return [] }
            let parsed = values.compactMap(Wire.integer)
            guard parsed.count == values.count, Set(parsed).count == parsed.count,
                  parsed.allSatisfy({ (0...5).contains($0) }), positions.map({ range in parsed.allSatisfy(range.contains) }) ?? true else { return [] }
            positions = parsed
        }
        return positions?.sorted() ?? []
    }
    var cleaningKey: String { attributes["EvaporatorClean"]?["key"] as? String ?? "acec" }
    func cleaning(_ status: ACStatus) -> Bool? {
        guard let value = status.fields[cleaningKey] else { return nil }
        if Wire.same(value, "off") || (value as? NSNumber)?.doubleValue == 0 { return false }
        if Wire.same(value, "on") || (value as? NSNumber)?.doubleValue == 1 { return true }
        return nil
    }
    func blocked(_ action: ControlAction, status: ACStatus) -> String? {
        guard supported, key(action) != nil else { return "This control is unavailable for the selected model." }
        guard status.online == true else { return "The AC is offline or its connection state is unknown. Refresh first." }
        guard let current = value(action, status), !current.isEmpty else { return "This setting was not reported. Refresh first." }
        if [.power,.display,.eco,.powerful].contains(action), !["on", "off"].contains(current.lowercased()) {
            return "The current switch state is unknown."
        }
        if [.mode,.fan].contains(action), !choices(action).contains(current.lowercased()) {
            return "The reported setting is not recognized by this model."
        }
        if action == .capacity, Wire.integer(current).map({ choices(.capacity).contains(String($0)) }) != true {
            return "The reported capacity is unknown."
        }
        if [.vertical,.horizontal].contains(action), Wire.integer(current).map({ positions(action).contains($0) }) != true {
            return "The airflow position or capability is unknown."
        }
        if [.power,.display].contains(action) {
            guard ["on", "off"].contains(current.lowercased()) else { return "The current switch state is unknown." }
        } else {
            guard isOn(.power, status) == true else { return "Turn on the AC to change this setting." }
            guard cleaning(status) == false else { return "Controls are unavailable during cleaning or while cleaning state is unknown." }
            guard let mode = value(.mode, status)?.lowercased(), choices(.mode).contains(mode) else { return "Refresh the applied mode before changing settings." }
            if action == .temperature && (mode == "fan" || Wire.number(current).map({ bounds(mode: mode)?.contains($0) == true }) != true) { return "Temperature is unavailable in this mode or its reported value is unknown." }
            if action == .fan && mode == "dry" { return "Dry mode manages the fan automatically." }
            if [.eco,.powerful].contains(action) && mode != "cool" { return "Available in Cool mode only." }
        }
        if [.power,.eco,.powerful].contains(action), let sleep = Wire.text(status.fields["acsp"]), !["none", "off"].contains(sleep.lowercased()) {
            return "An active sleep profile must be stopped in MirAIe first."
        }
        if action == .capacity, let jet = Wire.text(status.fields["acjs"]), jet.lowercased() != "off" { return "Stop JetStream in MirAIe before changing capacity." }
        return nil
    }
    func command(_ action: ControlAction, value: ControlValue, status: ACStatus) throws -> ControlCommand {
        if let reason = blocked(action, status: status) { throw MiraieError.blocked(reason) }
        guard let key = key(action) else { throw MiraieError.blocked("Control unavailable.") }
        switch (action, value) {
        case (.temperature, .number(let v)):
            guard let mode = self.value(.mode, status), bounds(mode: mode)?.contains(v) == true else { throw MiraieError.blocked("Choose a temperature in the advertised range.") }
        case (.vertical, .integer(let v)), (.horizontal, .integer(let v)):
            guard positions(action).contains(v) else { throw MiraieError.blocked("Choose an advertised airflow position.") }
        case (.capacity, .integer(let v)):
            guard choices(action).contains(String(v)) else { throw MiraieError.blocked("Choose an advertised capacity.") }
        case (.power, .text(let v)), (.display, .text(let v)), (.eco, .text(let v)), (.powerful, .text(let v)), (.mode, .text(let v)), (.fan, .text(let v)):
            guard choices(action).contains(v) else { throw MiraieError.blocked("This setting is not advertised by the AC.") }
        default: throw MiraieError.blocked("Invalid control value.")
        }
        return ControlCommand(action: action, key: key, value: value)
    }
    func plan(_ action: ControlAction, value: ControlValue, status: ACStatus) throws -> [ControlCommand] {
        let final = try command(action, value: value, status: status)
        if action == .eco && value == .text("on") {
            return [final, try command(.temperature, value: .number(26), status: status)]
        }
        if action == .powerful && value == .text("on") {
            guard let capacity = self.value(.capacity, status), let number = Double(capacity) else { throw MiraieError.blocked("Refresh capacity before enabling Powerful.") }
            if number != 0 { return [try command(.capacity, value: .integer(0), status: status), final] }
        }
        if action == .capacity && value != .integer(0) {
            guard let powerful = isOn(.powerful, status) else { throw MiraieError.blocked("Refresh Powerful before changing capacity.") }
            if powerful { return [try command(.powerful, value: .text("off"), status: status), final] }
        }
        return [final]
    }
}
