import XCTest
@testable import OpenMirAIeMac

func device(model: String = "140013", routed: Bool = false) -> MiraieDevice {
    MiraieDevice(id: "synthetic-ac", name: "Test AC", home: "Test Home", space: "Test Room", modelID: model, category: "AC", routed: routed)
}
func metadata() -> [String: Any] {
    let mapping: [(ControlAction, String)] = [(.power,"ps"),(.mode,"acmd"),(.temperature,"actmp"),(.fan,"acfs"),(.display,"acdc"),(.eco,"acem"),(.powerful,"acpm"),(.capacity,"cnv"),(.vertical,"acvs"),(.horizontal,"achs")]
    let caps = mapping.map { action, key -> [String: Any] in
        var attr: [String: Any] = ["key": key]
        switch action {
        case .mode: attr["values"] = ["auto","cool","dry","fan","heat"]
        case .fan: attr["values"] = ["auto","quiet","low","medium","high"]
        case .display,.eco: attr["values"] = ["on","off"]
        case .capacity: attr["intValues"] = [110,100,90,80,70,60,55,50,40,0]
        case .temperature:
            attr["range"] = ["min":[["unit":"fahrenheit","value":61],["unit":"celcius","value":16]],
                             "max":[["unit":"celcius","value":30]],"increment":[["unit":"celsius","value":1]]]
        case .vertical,.horizontal:
            attr["range"] = ["min":[["value":0]],"max":[["value":5]],"increment":[["value":1]]]
        default: break // Actual model omits enum values on PowerStatus and PowerfulMode.
        }
        return ["capability":action.capability,"attributes":[attr]]
    }
    return ["modelId":"140013","controlCapabilities":caps]
}
func baseline(_ extra: [String: Any] = [:]) -> [String: Any] {
    var value: [String: Any] = ["ps":"on","acmd":"cool","actmp":"24.0","acfs":"auto","acdc":"on","acem":"off","acpm":"off","cnv":0,"acvs":2,"achs":1,"acec":"off","acsp":"none","onlineStatus":"true","rmtmp":"24.12"]
    value.merge(extra) { _, new in new }
    return value
}
final class MemorySession: SessionStorage {
    var current: MiraieSession? = MiraieSession(accessToken: "fixture-access", refreshToken: "fixture-refresh")
    func load() throws -> MiraieSession? { current }
    func save(_ session: MiraieSession) throws { current = session }
    func clear() throws { current = nil }
}
@MainActor final class FakeCloud: MiraieTransport {
    var requests: [URLRequest] = []
    var fields = baseline()
    var echo = true
    var failControl = false
    var failFirstRead = false
    var failMutationAuthorization = false
    var expireAfterFirstControl = false
    var onlyEchoEco = false
    var didExpire = false
    var writes = 0
    var handler: ((URLRequest) throws -> (Data, Int))?

    func response(_ value: Any, _ status: Int = 200) throws -> (Data, Int) {
        (try JSONSerialization.data(withJSONObject: value), status)
    }
    func send(_ request: URLRequest) async throws -> (Data, Int) {
        requests.append(request)
        if let handler { return try handler(request) }
        let path = request.url!.path
        if path.hasSuffix("/tokenRefresh") { return try response(["accessToken":"fixture-rotated","refreshToken":"fixture-refresh-rotated"]) }
        if path.hasSuffix("/login") { return try response(["accessToken":"fixture-login","refreshToken":"fixture-login-refresh"]) }
        if path.contains("deviceModels") { return try response(metadata()) }
        if path.hasSuffix("/mobile/status") {
            if failFirstRead && !didExpire { didExpire = true; return try response([:],401) }
            if expireAfterFirstControl && writes > 0 && !didExpire { didExpire = true; return try response([:],401) }
            return try response(fields)
        }
        if path.hasSuffix("deviceManagement/control") {
            writes += 1
            if failMutationAuthorization { return try response([:],401) }
            if failControl { throw URLError(.timedOut) }
            let body = try XCTUnwrap(try JSONSerialization.jsonObject(with: request.httpBody!) as? [String: Any])
            let params = try XCTUnwrap(body["control_params"] as? [String: Any])
            if echo {
                for (key, value) in params where !["cnt","sid","ki"].contains(key) {
                    if !onlyEchoEco || key == "acem" { fields[key] = value }
                }
            }
            return try response([:])
        }
        if path.hasSuffix("homeManagement/homes") { return try response([]) }
        throw MiraieError.malformed
    }
}

@MainActor
final class ProtocolTests: XCTestCase {
    func testNativeKeychainRoundTripUsesIsolatedService() throws {
        let store = KeychainSessionStore(service: "in.openmiraie.macos.tests." + UUID().uuidString)
        defer { try? store.clear() }
        XCTAssertNil(try store.load())
        try store.save(MiraieSession(accessToken: "fixture-access", refreshToken: "fixture-refresh"))
        XCTAssertEqual(try store.load()?.accessToken,"fixture-access")
        try store.save(MiraieSession(accessToken: "fixture-rotated", refreshToken: "fixture-next"))
        XCTAssertEqual(try store.load()?.refreshToken,"fixture-next")
        try store.clear()
        XCTAssertNil(try store.load())
    }
    func testBooleanDoesNotMasqueradeAsNumericCommandReadback() {
        XCTAssertFalse(Wire.same(true,1))
        XCTAssertFalse(Wire.same(false,"0"))
        XCTAssertTrue(Wire.same(true,true))
    }
    func testDiscoveryPreservesHierarchyAndDoesNotTreatOtherFamiliesAsAC() throws {
        let json: [[String: Any]] = [["homeName":"Test Home","spaces":[["spaceName":"Test Space","devices":[
            ["deviceId":"fixture-1","deviceName":"AC","category":"AC","modelId":140013],
            ["deviceId":"fixture-2","deviceName":"Light","category":"LIGHT","modelId":"other"]
        ]]]]]
        let found = try MiraieDevice.discover(json)
        XCTAssertEqual(found.count,2)
        XCTAssertEqual(found[0].home,"Test Home")
        XCTAssertEqual(found[0].title,"Test Space")
        XCTAssertTrue(found[0].supportsReviewedAdapter)
        XCTAssertFalse(found[1].supportsReviewedAdapter)
    }
    func testUnknownModelsAndParentRoutedDevicesFailClosed() throws {
        XCTAssertFalse(try ACProfile(metadata(),device:device(model:"other")).supported)
        XCTAssertFalse(try ACProfile(metadata(),device:device(routed:true)).supported)
    }
    func testPowerAndDisplayUseIndependentModelKeysAndRegistryIndexes() throws {
        let profile = try ACProfile(metadata(),device:device())
        let state = try ACStatus(baseline(["ps":"off"]))
        let power = try profile.command(.power,value:.text("on"),status:state)
        let display = try profile.command(.display,value:.text("off"),status:state)
        XCTAssertEqual(power.key,"ps"); XCTAssertEqual(power.parameters["ki"] as? Int,1)
        XCTAssertEqual(display.key,"acdc"); XCTAssertEqual(display.parameters["ki"] as? Int,8)
        XCTAssertNil(display.parameters["ps"])
    }
    func testTemperatureSelectsCelsiusAndRejectsInvalidValues() throws {
        let profile = try ACProfile(metadata(),device:device())
        let state = try ACStatus(baseline())
        XCTAssertEqual(profile.bounds(mode:"cool"),TemperatureBounds(minimum:16,maximum:30,step:1))
        XCTAssertThrowsError(try profile.command(.temperature,value:.number(24.5),status:state))
        XCTAssertThrowsError(try profile.command(.temperature,value:.number(31),status:state))
        XCTAssertEqual(try profile.command(.temperature,value:.number(25),status:state).parameters["actmp"] as? String,"25.0")
    }
    func testNoFallbackTemperatureBoundsOrAmbiguousAttribute() throws {
        var model = metadata()
        var caps = model["controlCapabilities"] as! [[String: Any]]
        let i = caps.firstIndex { $0["capability"] as? String == "ACTemperature" }!
        caps[i]["attributes"] = [["key":"actmp"]]
        model["controlCapabilities"] = caps
        XCTAssertNil(try ACProfile(model,device:device()).bounds(mode:"cool"))
        caps.append(caps[i]); model["controlCapabilities"] = caps
        XCTAssertNil(try ACProfile(model,device:device()).key(.temperature))
    }
    func testUnsupportedModesAndCapacityValuesAreExcluded() throws {
        let profile = try ACProfile(metadata(),device:device())
        XCTAssertEqual(profile.choices(.mode),["auto","cool","dry","fan"])
        XCTAssertFalse(profile.choices(.capacity).contains("55"))
    }
    func testAvailabilityAndFractionalRoomTemperatureStayHonest() throws {
        for value: Any in [true,1,"true","online"] { XCTAssertEqual(try ACStatus(baseline(["onlineStatus":value])).online,true) }
        for value: Any in [false,0,"false","offline"] { XCTAssertEqual(try ACStatus(baseline(["onlineStatus":value])).online,false) }
        XCTAssertNil(try ACStatus(baseline(["onlineStatus":"on"])).online)
        XCTAssertEqual(try ACStatus(baseline()).roomTemperature,24.12)
        XCTAssertNil(try ACStatus(baseline(["rmtmp":-1])).roomTemperature)
        XCTAssertNil(try ACStatus(baseline(["rmtmp":NSNull()])).roomTemperature)
    }
    func testStatusUnwrapsOnlyWrapperOnlyEnvelopes() throws {
        XCTAssertEqual(try ACStatus(["data":["status":baseline()]]).roomTemperature,24.12)
        let full: [String: Any] = ["ps":"on","data":["rmtmp":99]]
        XCTAssertNil(try ACStatus(full).roomTemperature)
        XCTAssertThrowsError(try ACStatus(["data":[]]))
    }
    func testDryFanAndCleaningGuards() throws {
        let p = try ACProfile(metadata(),device:device())
        XCTAssertNotNil(p.blocked(.fan,status:try ACStatus(baseline(["acmd":"dry"]))))
        XCTAssertNotNil(p.blocked(.temperature,status:try ACStatus(baseline(["acmd":"fan"]))))
        XCTAssertNotNil(p.blocked(.powerful,status:try ACStatus(baseline(["acmd":"auto"]))))
        XCTAssertNotNil(p.blocked(.temperature,status:try ACStatus(baseline(["acec":"on"]))))
        XCTAssertNotNil(p.blocked(.temperature,status:try ACStatus(baseline(["acec":NSNull()]))))
        XCTAssertNotNil(p.blocked(.power,status:try ACStatus(baseline(["onlineStatus":"unknown"]))))
    }
    func testSwingUsesAdvertisedIntegerPositionsOnly() throws {
        let p = try ACProfile(metadata(),device:device())
        XCTAssertEqual(p.positions(.horizontal),[0,1,2,3,4,5])
        XCTAssertThrowsError(try p.command(.horizontal,value:.integer(6),status:ACStatus(baseline())))
        XCTAssertThrowsError(try p.command(.horizontal,value:.number(2),status:ACStatus(baseline())))
    }
    func testCoupledPlansPreflightEveryStep() throws {
        let p = try ACProfile(metadata(),device:device())
        let eco = try p.plan(.eco,value:.text("on"),status:ACStatus(baseline()))
        XCTAssertEqual(eco.map(\.action),[.eco,.temperature])
        XCTAssertEqual(eco[1].value,.number(26))
        let powerful = try p.plan(.powerful,value:.text("on"),status:ACStatus(baseline(["cnv":80])))
        XCTAssertEqual(powerful.map(\.action),[.capacity,.powerful])
        let capacity = try p.plan(.capacity,value:.integer(80),status:ACStatus(baseline(["acpm":"on"])))
        XCTAssertEqual(capacity.map(\.action),[.powerful,.capacity])
        XCTAssertThrowsError(try p.plan(.eco,value:.text("on"),status:ACStatus(baseline(["acec":"on"]))))
        XCTAssertThrowsError(try p.plan(.power,value:.text("off"),status:ACStatus(baseline(["acsp":"active-profile"]))))
        XCTAssertThrowsError(try p.plan(.capacity,value:.integer(80),status:ACStatus(baseline(["acjs":"on"]))))
    }
    func testAuthenticationPreservesMobileFormatAndStoresTokensOnly() async throws {
        let storage = MemorySession(), cloud = FakeCloud()
        let client = try MiraieClient(storage:storage,transport:cloud)
        try await client.signIn(identity:"+910000000000",password:"fixture-password")
        let body = try JSONSerialization.jsonObject(with:cloud.requests[0].httpBody!) as! [String:Any]
        XCTAssertEqual(body["mobile"] as? String,"+910000000000")
        XCTAssertNil(body["email"])
        let saved = try JSONSerialization.jsonObject(with:JSONEncoder().encode(storage.current)) as! [String:Any]
        XCTAssertEqual(Set(saved.keys),["accessToken","refreshToken"])
    }
    func testReadRefreshesOnceAndKeepsRotatedRefreshToken() async throws {
        let storage=MemorySession(), cloud=FakeCloud(); cloud.failFirstRead=true
        let client=try MiraieClient(storage:storage,transport:cloud)
        _ = try await client.status(device())
        XCTAssertEqual(cloud.requests.filter { $0.url!.path.hasSuffix("/tokenRefresh") }.count,1)
        XCTAssertEqual(cloud.requests.filter { $0.url!.path.hasSuffix("/mobile/status") }.count,2)
        XCTAssertEqual(storage.current?.refreshToken,"fixture-refresh-rotated")
    }
    func testMutationAuthorizationFailureIsNeverReplayed() async throws {
        let storage=MemorySession(), cloud=FakeCloud(); cloud.failMutationAuthorization=true
        let client=try MiraieClient(storage:storage,transport:cloud)
        let p=try ACProfile(metadata(),device:device())
        do { try await client.send(p.command(.display,value:.text("off"),status:ACStatus(baseline())),device:device()); XCTFail("Expected expiry") }
        catch { XCTAssertFalse(client.isSignedIn) }
        XCTAssertEqual(cloud.writes,1)
        XCTAssertFalse(cloud.requests.contains { $0.url!.path.hasSuffix("/tokenRefresh") })
    }
    func testConfirmedWriteReadsActualStateAfterThreeSecondDelay() async throws {
        let cloud=FakeCloud(), client=try MiraieClient(storage:MemorySession(),transport:cloud)
        var delays:[Double]=[], observations:[ACStatus]=[]
        let runner=CommandExecutor(client:client,wait:{ delays.append($0) })
        let result=try await runner.execute(action:.display,value:.text("off"),device:device()) { _, status in observations.append(status) }
        XCTAssertTrue(result.complete); XCTAssertEqual(cloud.writes,1); XCTAssertEqual(delays,[3])
        XCTAssertEqual(observations.count,2)
        XCTAssertEqual(result.status.fields["acdc"] as? String,"off")
    }
    func testUnconfirmedWriteIsNotRepeatedAndRetainsActualReport() async throws {
        let cloud=FakeCloud(); cloud.echo=false
        let client=try MiraieClient(storage:MemorySession(),transport:cloud)
        var delays:[Double]=[]
        let result=try await CommandExecutor(client:client,wait:{ delays.append($0) }).execute(action:.temperature,value:.number(25),device:device(),observed:{_,_ in})
        XCTAssertFalse(result.complete); XCTAssertEqual(cloud.writes,1); XCTAssertEqual(delays,[3,1.2,1.8])
        XCTAssertEqual(result.status.fields["actmp"] as? String,"24.0")
    }
    func testAmbiguousDeliveryIsNotRetried() async throws {
        let cloud=FakeCloud(); cloud.failControl=true
        let client=try MiraieClient(storage:MemorySession(),transport:cloud)
        let result=try await CommandExecutor(client:client,wait:{_ in}).execute(action:.display,value:.text("off"),device:device(),observed:{_,_ in})
        XCTAssertFalse(result.complete); XCTAssertEqual(cloud.writes,1)
        XCTAssertEqual(result.status.fields["acdc"] as? String,"on")
    }
    func testPartialEcoKeepsFirstConfirmedEffectAndStops() async throws {
        let cloud=FakeCloud(); cloud.onlyEchoEco=true
        let client=try MiraieClient(storage:MemorySession(),transport:cloud)
        let result=try await CommandExecutor(client:client,wait:{_ in}).execute(action:.eco,value:.text("on"),device:device(),observed:{_,_ in})
        XCTAssertEqual(result.confirmed,1); XCTAssertEqual(result.total,2); XCTAssertFalse(result.complete)
        XCTAssertEqual(result.status.fields["acem"] as? String,"on")
        XCTAssertEqual(result.status.fields["actmp"] as? String,"24.0")
        XCTAssertEqual(cloud.writes,2)
    }
    func testSessionRotationStopsDependentMutation() async throws {
        let cloud=FakeCloud(); cloud.expireAfterFirstControl=true
        let client=try MiraieClient(storage:MemorySession(),transport:cloud)
        let result=try await CommandExecutor(client:client,wait:{_ in}).execute(action:.eco,value:.text("on"),device:device(),observed:{_,_ in})
        XCTAssertEqual(result.confirmed,1); XCTAssertFalse(result.complete); XCTAssertEqual(cloud.writes,1)
        XCTAssertEqual(result.status.fields["actmp"] as? String,"24.0")
    }
    func testEncodedIdentifierCannotChangeRequestPath() {
        XCTAssertEqual(MiraieClient.segment("test/id?secret=none"),"test%2Fid%3Fsecret%3Dnone")
    }
}
