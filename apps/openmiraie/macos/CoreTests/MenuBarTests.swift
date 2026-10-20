import XCTest
import SwiftUI
@testable import OpenMirAIeMac

@MainActor
final class MenuBarTests: XCTestCase {
    func testClosingPanelCancelsTemperatureDraftWithoutSending() async throws {
        let cloud = FakeCloud()
        let store = try await connected(cloud)
        let draft = TemperatureDraft()
        draft.increment(1, store: store)
        draft.cancel()
        store.panelVisible = false
        try await Task.sleep(for: .milliseconds(500))
        XCTAssertNil(draft.value)
        XCTAssertEqual(cloud.writes, 0)
    }
    func testKeyboardTemperatureChangesCoalesceToOneWrite() async throws {
        let cloud = FakeCloud()
        let store = try await connected(cloud)
        let draft = TemperatureDraft()
        draft.increment(1, store: store)
        draft.increment(1, store: store)
        draft.increment(-1, store: store)
        XCTAssertEqual(draft.value, 25)
        try await Task.sleep(for: .milliseconds(650))
        XCTAssertEqual(cloud.writes, 1)
        XCTAssertEqual(store.requestedTarget, 25)
        store.setActive(false)
    }
    func testSliderOnlyCommitsItsFinalValueOnce() async throws {
        let cloud = FakeCloud()
        let store = try await connected(cloud)
        let draft = TemperatureDraft()
        draft.change(22); draft.change(23); draft.change(25)
        XCTAssertEqual(cloud.writes, 0)
        draft.commit(store: store)
        await Task.yield()
        XCTAssertEqual(store.requestedTarget, 25)
        draft.commit(store: store)
        try await Task.sleep(for: .milliseconds(50))
        XCTAssertEqual(cloud.writes, 1)
        store.setActive(false)
    }
    func testUnconfirmedCommandBlocksFurtherWritesUntilReadRefresh() async throws {
        let cloud = FakeCloud(); cloud.failControl = true
        let store = try await connected(cloud)
        store.request(.display, .text("off"))
        try await Task.sleep(for: .milliseconds(50))
        XCTAssertTrue(store.unconfirmed)
        XCTAssertFalse(store.can(.power))
        XCTAssertEqual(store.display, true)
        XCTAssertEqual(store.menuTitle, "")
        await store.refresh()
        XCTAssertFalse(store.unconfirmed)
        XCTAssertTrue(store.can(.power))
        XCTAssertEqual(cloud.writes, 1)
    }
    func testReadFailureRetainsReportedStateAndDisablesControls() async throws {
        let cloud = FakeCloud()
        let store = try await connected(cloud)
        cloud.handler = { _ in throw URLError(.notConnectedToInternet) }
        await store.refresh()
        XCTAssertTrue(store.isStale)
        XCTAssertEqual(store.target, 24)
        XCTAssertFalse(store.can(.temperature))
    }
    func testChangingRoomsClearsPreviousReportOnFailure() async throws {
        let cloud = FakeCloud()
        let store = try await connected(cloud, twoRooms: true)
        cloud.handler = { _ in throw URLError(.notConnectedToInternet) }
        await store.choose(store.acDevices[1])
        XCTAssertEqual(store.title, "Other Room")
        XCTAssertNil(store.reported)
        XCTAssertNil(store.target)
    }
    func testSignOutClearsPresentationWithoutApplianceWrite() async throws {
        let cloud = FakeCloud()
        let store = try await connected(cloud)
        store.signOut()
        XCTAssertFalse(store.signedIn)
        XCTAssertNil(store.selected)
        XCTAssertNil(store.reported)
        XCTAssertEqual(cloud.writes, 0)
    }
    func testEcoAlreadyOnStillAppliesItsUnmetTargetEffect() async throws {
        let cloud = FakeCloud(); cloud.fields["acem"] = "on"
        let client = try MiraieClient(storage: MemorySession(), transport: cloud)
        let outcome = try await CommandExecutor(client: client, wait: { _ in }).execute(action: .eco, value: .text("on"), device: device(), observed: { _,_ in })
        XCTAssertTrue(outcome.complete)
        XCTAssertEqual(outcome.status.fields["actmp"] as? String, "26.0")
        XCTAssertEqual(cloud.writes, 1)
    }
    func testMenuTitleDoesNotPresentMissingOrOfflineReadingsAsCurrent() {
        XCTAssertEqual(ACStore.preview().menuTitle, "24°")
        XCTAssertEqual(ACStore.preview(state: "off").menuTitle, "Off")
        for state in ["offline", "pending", "unconfirmed", "fan", "signed-out"] {
            XCTAssertEqual(ACStore.preview(state: state).menuTitle, "", state)
        }
        let stale = ACStore.preview(); stale.markStale()
        XCTAssertEqual(stale.menuTitle, "")
    }
    func testNativePanelRendersLightDarkAndRecoveryStates() async throws {
        let output = URL(fileURLWithPath: NSTemporaryDirectory()).appendingPathComponent("openmiraie-macos-review", isDirectory: true)
        try FileManager.default.createDirectory(at: output, withIntermediateDirectories: true)
        for state in ["ready", "signed-out", "offline", "off", "fan", "dry", "cleaning", "pending", "unconfirmed"] {
            for scheme: ColorScheme in [.light, .dark] {
                // ImageRenderer omits AppKit-backed ScrollView/TextField content on macOS.
                // Lay out and capture an actual native hosting view instead.
                let root = MenuPanel(store: .preview(state: state)).environment(\.colorScheme, scheme)
                let hosting = NSHostingView(rootView: root)
                let window = NSWindow(contentRect: NSRect(x: -10000, y: -10000, width: 360, height: 620), styleMask: .borderless, backing: .buffered, defer: false)
                window.isReleasedWhenClosed = false
                window.contentView = hosting
                window.appearance = NSAppearance(named: scheme == .dark ? .darkAqua : .aqua)
                window.orderFront(nil)
                try await Task.sleep(for: .milliseconds(100))
                hosting.layoutSubtreeIfNeeded()
                window.displayIfNeeded()
                let bitmap = try XCTUnwrap(hosting.bitmapImageRepForCachingDisplay(in: hosting.bounds))
                hosting.cacheDisplay(in: hosting.bounds, to: bitmap)
                window.orderOut(nil)
                let data = try XCTUnwrap(bitmap.representation(using: .png, properties: [:]))
                // Assert the central content exists, not merely an empty scroll viewport.
                var colors = Set<String>()
                for x in stride(from: 20, to: bitmap.pixelsWide - 20, by: 5) {
                    for y in stride(from: bitmap.pixelsHigh / 4, to: bitmap.pixelsHigh * 3 / 4, by: 5) {
                        if let c = bitmap.colorAt(x: x, y: y)?.usingColorSpace(.deviceRGB) {
                            colors.insert("\(Int(c.redComponent * 255)),\(Int(c.greenComponent * 255)),\(Int(c.blueComponent * 255))")
                        }
                    }
                }
                XCTAssertGreaterThan(colors.count, 15, "Native content missing for " + state)
                let name = state + (scheme == .dark ? "-dark" : "-light")
                try data.write(to: output.appendingPathComponent(name + ".png"))
                XCTAssertGreaterThanOrEqual(bitmap.pixelsWide, 360)
                XCTAssertGreaterThanOrEqual(bitmap.pixelsHigh, 620)
                let attachment = XCTAttachment(data: data, uniformTypeIdentifier: "public.png")
                attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
            }
        }
        print("Native panel review images: " + output.path)
    }
    private func connected(_ cloud: FakeCloud, twoRooms: Bool = false) async throws -> ACStore {
        let suite = "in.openmiraie.macos.tests." + UUID().uuidString
        let prefs = try XCTUnwrap(UserDefaults(suiteName: suite))
        defer { prefs.removePersistentDomain(forName: suite) }
        let originalHandler = cloud.handler
        cloud.handler = { request in
            if request.url!.path.hasSuffix("homeManagement/homes") {
                var devices: [[String:Any]] = [["deviceId":"synthetic-ac","deviceName":"Test AC","category":"AC","modelId":"140013"]]
                if twoRooms { devices.append(["deviceId":"synthetic-other","deviceName":"Other Room","category":"AC","modelId":"140013"]) }
                return try cloud.response([["homeName":"Test Home","spaces":[["devices":devices]]]])
            }
            cloud.handler = originalHandler
            defer { cloud.handler = nil }
            return try cloud.response([:])
        }
        let store = ACStore(client: try MiraieClient(storage: MemorySession(), transport: cloud), preferences: prefs)
        await store.discover()
        cloud.handler = nil
        await store.choose(try XCTUnwrap(store.acDevices.first))
        store.panelVisible = true
        return store
    }
}
