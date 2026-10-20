import SwiftUI
import AppKit
import Combine

extension Notification.Name {
    static let miraiePanelClosed = Notification.Name("in.openmiraie.macos.panelClosed")
}

@main
struct OpenMirAIeMacApp: App {
    @NSApplicationDelegateAdaptor(StatusBarController.self) private var delegate
    var body: some Scene { Settings { EmptyView() } }
}

@MainActor
final class StatusBarController: NSObject, NSApplicationDelegate, NSPopoverDelegate {
    private var item: NSStatusItem?
    private let popover = NSPopover()
    private var store: ACStore?
    private var observations = Set<AnyCancellable>()
    private var clock: Timer?

    func applicationDidFinishLaunching(_ notification: Notification) {
        // The XCTest host never opens a real session or creates a status item.
        guard ProcessInfo.processInfo.environment["OPENMIRAIE_NO_NETWORK"] != "1" else { return }
        NSApp.setActivationPolicy(.accessory)
        let store: ACStore
        #if DEBUG
        if CommandLine.arguments.contains("--preview") { store = ACStore.preview() }
        else { store = ACStore() }
        #else
        store = ACStore()
        #endif
        self.store = store
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        self.item = item
        item.button?.target = self
        item.button?.action = #selector(togglePanel)
        item.button?.image = NSImage(systemSymbolName: "snowflake", accessibilityDescription: "openMirAIe")
        item.button?.imagePosition = .imageLeading
        popover.behavior = .transient
        popover.delegate = self
        popover.contentSize = NSSize(width: 360, height: 620)
        popover.contentViewController = NSHostingController(rootView: MenuPanel(store: store))
        store.objectWillChange.receive(on: RunLoop.main).sink { [weak self] in
            // Published properties announce before assignment.
            DispatchQueue.main.async { self?.updateLabel() }
        }.store(in: &observations)
        NotificationCenter.default.publisher(for: UserDefaults.didChangeNotification)
            .sink { [weak self] _ in self?.updateLabel() }.store(in: &observations)
        NSWorkspace.shared.notificationCenter.publisher(for: NSWorkspace.willSleepNotification)
            .sink { [weak self] _ in self?.store?.setActive(false); self?.popover.performClose(nil) }.store(in: &observations)
        NSWorkspace.shared.notificationCenter.publisher(for: NSWorkspace.didWakeNotification)
            .sink { [weak self] _ in self?.store?.setActive(true); self?.store?.markStale(); self?.updateLabel() }.store(in: &observations)
        // Update freshness labels locally. This timer makes no network requests.
        clock = Timer.scheduledTimer(withTimeInterval: 15, repeats: true) { [weak self] _ in
            Task { @MainActor in self?.updateLabel() }
        }
        updateLabel()
    }
    private func updateLabel() {
        guard let store else { return }
        store.expireOldReport()
        let show = UserDefaults.standard.object(forKey: "showMenuTemperature") as? Bool ?? true
        item?.button?.title = show && !store.menuTitle.isEmpty ? " " + store.menuTitle : ""
        item?.button?.toolTip = "openMirAIe · " + (store.selected == nil ? "Control your AC" : store.title + " · " + store.status)
        item?.button?.setAccessibilityLabel("openMirAIe, " + store.status)
    }
    @objc private func togglePanel() {
        if popover.isShown { popover.performClose(nil); return }
        guard let button = item?.button, let store else { return }
        store.panelVisible = true
        NSApp.activate(ignoringOtherApps: true)
        popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        if UserDefaults.standard.object(forKey: "refreshOnOpen") as? Bool ?? true {
            Task {
                if store.devices.isEmpty { await store.bootstrap() }
                else if store.isStale { await store.refresh() }
            }
        }
    }
    func popoverWillClose(_ notification: Notification) {
        store?.panelVisible = false
        NotificationCenter.default.post(name: .miraiePanelClosed, object: nil)
    }
    func applicationWillTerminate(_ notification: Notification) { clock?.invalidate() }
}
