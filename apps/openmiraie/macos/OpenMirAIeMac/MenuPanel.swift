import SwiftUI

private enum PanelPage { case control, account, devices }
private struct PendingConfirmation: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let action: ControlAction
    let value: ControlValue
}

struct MenuPanel: View {
    @ObservedObject var store: ACStore
    @State private var page: PanelPage = .control
    @State private var confirmation: PendingConfirmation?
    @State private var confirmSignOut = false
    @AppStorage("showMenuTemperature") private var showTemperature = true
    @AppStorage("refreshOnOpen") private var refreshOnOpen = true
    @Environment(\.colorScheme) private var scheme
    private var canvas: Color { scheme == .dark ? Color(red: 0.10, green: 0.12, blue: 0.12) : Color(red: 0.97, green: 0.97, blue: 0.95) }

    var body: some View {
        VStack(spacing: 0) {
            header.padding(20)
            Divider()
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    if !store.signedIn { SignInPanel(store: store) }
                    else if page == .account { account }
                    else if page == .devices || store.selected == nil { devices }
                    else { controller }
                }
                .padding(20)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            Divider()
            footer.padding(.horizontal, 20).padding(.vertical, 12)
        }
        .frame(width: 360, height: 620)
        .background(canvas)
        .tint(Color(red: 0.14, green: 0.35, blue: 0.83))
        .alert(item: $confirmation) { item in
            Alert(title: Text(item.title), message: Text(item.message),
                  primaryButton: .default(Text("Apply")) { store.request(item.action, item.value) },
                  secondaryButton: .cancel())
        }
        .alert("Sign out of MirAIe?", isPresented: $confirmSignOut) {
            Button("Sign out", role: .destructive) { store.signOut(); page = .control }
            Button("Cancel", role: .cancel) { }
        } message: { Text("Your saved session and selected AC will be removed from this Mac.") }
        .onReceive(NotificationCenter.default.publisher(for: .miraiePanelClosed)) { _ in confirmation = nil }
        .onChange(of: store.signedIn) { _, _ in page = .control }
    }
    private var header: some View {
        HStack(spacing: 10) {
            if page != .control && store.signedIn {
                Button { page = .control } label: { Image(systemName: "chevron.left") }
                    .buttonStyle(.plain).help("Back to controls").accessibilityLabel("Back to controls")
            } else {
                Image(systemName: "snowflake").font(.system(size: 22)).foregroundStyle(.tint)
            }
            VStack(alignment: .leading, spacing: 3) {
                Text(page == .account ? "Settings" : "openMirAIe").font(.system(size: 16, weight: .semibold))
                Text(store.isPreview ? "Preview · no cloud connection" : "A little more comfortable.").font(.system(size: 11)).foregroundStyle(.secondary)
            }
            Spacer()
            if store.loading || store.pending != nil { ProgressView().controlSize(.small).accessibilityLabel("Working") }
            else if store.signedIn && page == .control {
                Button { Task { await store.refresh() } } label: { Image(systemName: "arrow.clockwise") }
                    .buttonStyle(.plain).help("Refresh reported state").accessibilityLabel("Refresh reported state")
            }
        }
    }
    private var controller: some View {
        VStack(spacing: 18) {
            HStack(alignment: .center) {
                Button { page = .devices } label: {
                    VStack(alignment: .leading, spacing: 5) {
                        HStack { Text(store.title).font(.system(size: 19, weight: .semibold)).lineLimit(1); Image(systemName: "chevron.down").font(.caption) }
                        Text(store.selected?.home ?? "").font(.caption).foregroundStyle(.secondary)
                    }
                }.buttonStyle(.plain).disabled(store.loading || store.pending != nil).help("Choose air conditioner")
                Spacer(minLength: 8)
                Button { if let power = store.power { store.request(.power, .toggle(!power)) } } label: {
                    Image(systemName: "power").font(.system(size: 21, weight: .medium))
                        .frame(width: 44, height: 44)
                        .foregroundStyle(store.power == true ? Color.white : Color.primary)
                        .background(store.power == true ? Color.accentColor : Color.primary.opacity(0.07), in: Circle())
                }
                .buttonStyle(.plain).disabled(!store.can(.power))
                .accessibilityLabel(store.power == true ? "Turn AC off" : "Turn AC on")
                .accessibilityValue(store.power.map { $0 ? "On" : "Off" } ?? "Not reported")
            }
            HStack(spacing: 6) {
                Circle().fill(store.online == true && !store.isStale ? Color.green : Color.secondary).frame(width: 6, height: 6)
                Text(store.status).font(.caption)
                Spacer()
                if let date = store.reported?.observedAt {
                    TimelineView(.periodic(from: .now, by: 15)) { _ in Text(date, style: .relative).font(.caption2).foregroundStyle(.secondary) }
                        .help("Time since the last cloud report was fetched")
                }
            }.foregroundStyle(.secondary)
            if store.profile?.supported == false {
                Label("This model is available for inspection. Controls need a reviewed model adapter.", systemImage: "info.circle")
                    .font(.callout).foregroundStyle(.secondary)
                if let room = store.reported?.roomTemperature { reading("Room", room.formatted(.number.precision(.fractionLength(1))) + " °C") }
            } else if store.mode == "fan" {
                VStack(spacing: 10) {
                    Image(systemName: "fan").font(.system(size: 46, weight: .light)).foregroundStyle(.tint)
                    Text("Just the breeze.").font(.title3)
                    Text("Temperature is managed in other modes.").font(.caption).foregroundStyle(.secondary)
                }.frame(maxWidth: .infinity).padding(.vertical, 25)
            } else {
                TemperatureEditor(store: store)
            }
            VStack(spacing: 0) {
                selection("Mode", symbol: "snowflake", action: .mode)
                Divider().padding(.horizontal, 12)
                selection("Fan", symbol: "fan", action: .fan)
            }.background(.background, in: RoundedRectangle(cornerRadius: 14))
            HStack(spacing: 8) {
                utility("Display", symbol: "lightbulb", action: .display, on: store.display)
                utility("Eco", symbol: "leaf", action: .eco, on: store.eco)
                utility("Powerful", symbol: "bolt", action: .powerful, on: store.powerful)
            }
            DisclosureGroup {
                VStack(spacing: 0) {
                    position("Vertical", action: .vertical)
                    position("Horizontal", action: .horizontal)
                    selection("Capacity", symbol: "gauge.with.dots.needle.50percent", action: .capacity)
                    Text("Positions follow the reviewed AC model. Left and right face the unit.")
                        .font(.caption2).foregroundStyle(.secondary).padding(10)
                }.padding(.top, 8)
            } label: { Label("Airflow & capacity", systemImage: "wind").font(.system(size: 12, weight: .medium)) }
            if !store.feedback.isEmpty {
                HStack(alignment: .top, spacing: 7) {
                    Image(systemName: store.unconfirmed || store.isStale ? "info.circle" : "checkmark.circle")
                    Text(store.feedback).fixedSize(horizontal: false, vertical: true)
                }.font(.caption).foregroundStyle(.secondary).frame(maxWidth: .infinity, alignment: .leading)
            }
            if store.unconfirmed || store.isStale {
                Button("Refresh status") { Task { await store.refresh() } }.disabled(store.loading || store.pending != nil)
            }
        }
    }
    private func selection(_ name: String, symbol: String, action: ControlAction) -> some View {
        HStack {
            Label(name, systemImage: symbol).font(.system(size: 12))
            Spacer()
            Menu {
                ForEach(store.choices(action), id: \.self) { value in
                    Button {
                        if action == .capacity, let number = Int(value) { request(action, .integer(number)) }
                        else { request(action, .text(value)) }
                    } label: {
                        if Wire.same(store.value(action), value) { Label(choiceLabel(action, value), systemImage: "checkmark") }
                        else { Text(choiceLabel(action, value)) }
                    }
                }
            } label: { Text(choiceLabel(action, store.value(action))).font(.system(size: 12, weight: .medium)) }
                .menuStyle(.borderlessButton).fixedSize().disabled(!store.can(action) || store.choices(action).isEmpty)
                .accessibilityLabel(name).accessibilityValue(choiceLabel(action, store.value(action)))
        }.padding(12)
    }
    private func choiceLabel(_ action: ControlAction, _ value: String?) -> String {
        action == .capacity ? store.capacityLabel(value) : store.label(value)
    }
    private func position(_ name: String, action: ControlAction) -> some View {
        HStack {
            Text(name).font(.system(size: 12))
            Spacer()
            Menu {
                ForEach(store.positions(action), id: \.self) { code in
                    Button(store.positionLabel(String(code))) { store.request(action, .integer(code)) }
                }
            } label: { Text(store.positionLabel(store.value(action))).font(.system(size: 12, weight: .medium)) }
                .menuStyle(.borderlessButton).fixedSize().disabled(!store.can(action))
                .accessibilityLabel(name + " airflow")
        }.padding(12)
    }
    private func utility(_ name: String, symbol: String, action: ControlAction, on: Bool?) -> some View {
        Button { if let on { request(action, .toggle(!on)) } } label: {
            VStack(spacing: 6) {
                Image(systemName: symbol).font(.system(size: 17))
                Text(name).font(.system(size: 11, weight: .medium))
                Text(on.map { $0 ? "On" : "Off" } ?? "Unknown").font(.system(size: 10)).foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity).padding(.vertical, 12)
            .foregroundStyle(on == true ? Color.accentColor : Color.primary)
            .background(on == true ? Color.accentColor.opacity(0.1) : Color.primary.opacity(0.04), in: RoundedRectangle(cornerRadius: 12))
        }.buttonStyle(.plain).disabled(!store.can(action))
            .accessibilityLabel(name).accessibilityValue(on.map { $0 ? "On" : "Off" } ?? "Not reported")
    }
    private func request(_ action: ControlAction, _ value: ControlValue) {
        if Wire.same(store.value(action), value.wire) { return }
        if action == .eco && value == .text("on") {
            confirmation = PendingConfirmation(title: "Enable Eco?", message: "Eco also sets the target to 26 °C. Turning Eco off later keeps that target.", action: action, value: value)
        } else if action == .powerful && value == .text("on") && Wire.number(store.value(.capacity)) != 0 {
            confirmation = PendingConfirmation(title: "Enable Powerful?", message: "Capacity will first return to Normal. Each change is checked before continuing.", action: action, value: value)
        } else if action == .capacity && value != .integer(0) && store.powerful == true {
            confirmation = PendingConfirmation(title: "Change capacity?", message: "Powerful will turn off before applying the selected capacity.", action: action, value: value)
        } else { store.request(action, value) }
    }
    private var devices: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Your air conditioners").font(.title3.weight(.semibold))
            Text("Choose the AC you want in your menu bar.").font(.callout).foregroundStyle(.secondary)
            ForEach(store.acDevices) { device in
                Button {
                    Task { await store.choose(device); page = .control }
                } label: {
                    HStack {
                        Image(systemName: "air.conditioner.horizontal").font(.title2).foregroundStyle(.tint)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(device.title).font(.system(size: 13, weight: .medium))
                            Text(device.home + " · " + device.name).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: store.selected?.id == device.id ? "checkmark.circle.fill" : "chevron.right").foregroundStyle(.secondary)
                    }.padding(12).background(.background, in: RoundedRectangle(cornerRadius: 12))
                }.buttonStyle(.plain).disabled(store.loading || store.pending != nil)
            }
            if store.acDevices.isEmpty {
                Text(store.loading ? "Finding your ACs…" : (store.feedback.isEmpty ? "No AC selected. Find your devices to get started." : store.feedback)).font(.callout).foregroundStyle(.secondary)
            }
            Button("Find devices") { Task { await store.discover() } }.disabled(store.loading || store.pending != nil)
        }
    }
    private var account: some View {
        VStack(alignment: .leading, spacing: 20) {
            Toggle("Show target in menu bar", isOn: $showTemperature).toggleStyle(.switch)
            Toggle("Refresh stale status when opened", isOn: $refreshOnOpen).toggleStyle(.switch)
            Text("Status refreshes on demand and after changes. There is no background cloud polling or history recording.").font(.caption).foregroundStyle(.secondary)
            Divider()
            if let selected = store.selected {
                Text(selected.title).font(.headline)
                reading("Model", selected.modelID)
                Text("Controls are reviewed for model 140013 (CS-CU-NU18BKY5WX). This Mac app still needs live device verification.").font(.caption).foregroundStyle(.secondary)
            }
            Button("Choose another AC") { page = .devices }
            Button("Sign out…", role: .destructive) { confirmSignOut = true }.disabled(store.loading || store.pending != nil)
            Divider()
            Text("openMirAIe · 0.1").font(.caption.weight(.medium))
            Text("An independent MirAIe India client. Not affiliated with Panasonic. Session tokens stay in this Mac’s Keychain; your password is never saved.").font(.caption).foregroundStyle(.secondary)
        }.font(.system(size: 12))
    }
    private func reading(_ title: String, _ value: String) -> some View { HStack { Text(title); Spacer(); Text(value) }.font(.caption) }
    private var footer: some View {
        HStack {
            if store.signedIn {
                Button { page = page == .account ? .control : .account } label: { Label("Settings", systemImage: "gearshape") }
                    .disabled(store.loading || store.pending != nil)
            } else { Text("MirAIe India").foregroundStyle(.secondary) }
            Spacer()
            Button("Quit") { NSApp.terminate(nil) }.keyboardShortcut("q")
        }.buttonStyle(.plain).font(.system(size: 11))
    }
}

private struct SignInPanel: View {
    @ObservedObject var store: ACStore
    @State private var identity = ""
    @State private var password = ""
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Image(systemName: "air.conditioner.horizontal").font(.system(size: 42, weight: .light)).foregroundStyle(.tint).padding(.top, 18)
            Text("Your AC.\nWithin reach.").font(.system(size: 30, weight: .semibold)).fixedSize(horizontal: false, vertical: true)
            Text("Sign in with your MirAIe India account to bring your room’s controls to the menu bar.").font(.callout).foregroundStyle(.secondary)
            VStack(alignment: .leading, spacing: 10) {
                TextField("Email or mobile number", text: $identity).textContentType(.username)
                SecureField("Password", text: $password).textContentType(.password).onSubmit(signIn)
            }.textFieldStyle(.roundedBorder).controlSize(.large).disabled(store.loading)
            Button(action: signIn) {
                HStack { Spacer(); Text(store.loading ? "Signing in…" : "Sign in"); Spacer() }
            }.buttonStyle(.borderedProminent).controlSize(.large).disabled(store.loading || identity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.isEmpty)
            if !store.signInError.isEmpty { Text(store.signInError).font(.caption).foregroundStyle(.secondary) }
            Label("Tokens are stored in Keychain. Your password is never saved.", systemImage: "lock.shield").font(.caption).foregroundStyle(.secondary)
        }
        .onDisappear { password = "" }
        .onReceive(NotificationCenter.default.publisher(for: .miraiePanelClosed)) { _ in password = "" }
    }
    private func signIn() {
        guard !store.loading, !password.isEmpty else { return }
        let secret = password
        password = ""
        Task { await store.signIn(identity: identity, password: secret) }
    }
}

/// Keeps a gesture/keyboard draft separate from reported state and pending delivery.
@MainActor
final class TemperatureDraft: ObservableObject {
    @Published private(set) var value: Double?
    private var task: Task<Void, Never>?
    func change(_ value: Double) { task?.cancel(); self.value = value }
    func increment(_ amount: Double, store: ACStore) {
        guard store.can(.temperature), let current = value ?? store.target, let bounds = store.temperatureBounds else { return }
        change(min(bounds.maximum, max(bounds.minimum, current + amount)))
        task = Task { [weak self, weak store] in
            do { try await Task.sleep(for: .milliseconds(450)); try Task.checkCancellation() }
            catch { return }
            guard let self, let store else { return }
            commit(store: store)
        }
    }
    func commit(store: ACStore) {
        let target = value
        cancel()
        guard store.panelVisible, store.can(.temperature), let target, target != store.target else { return }
        store.request(.temperature, .number(target))
    }
    func cancel() { task?.cancel(); task = nil; value = nil }
}

private struct TemperatureEditor: View {
    @ObservedObject var store: ACStore
    @StateObject private var draft = TemperatureDraft()
    var body: some View {
        VStack(spacing: 10) {
            Text(store.reported?.roomTemperature.map { "Room " + $0.formatted(.number.precision(.fractionLength(1))) + " °C" } ?? "Room temperature unavailable")
                .font(.system(size: 12)).foregroundStyle(.secondary)
            HStack(spacing: 10) {
                stepButton("minus", title: "Decrease temperature", direction: -1)
                Spacer(minLength: 0)
                VStack(spacing: 2) {
                    HStack(alignment: .firstTextBaseline, spacing: 0) {
                        Text((draft.value ?? store.requestedTarget ?? store.target).map { $0.formatted(.number.precision(.fractionLength(0...1))) } ?? "—")
                            .font(.system(size: 64, weight: .light, design: .rounded)).monospacedDigit()
                        Text("°").font(.system(size: 36, weight: .light)).baselineOffset(17)
                    }
                    Text(store.requestedTarget != nil ? "Applying · reported \(store.target.map { $0.formatted() + "°" } ?? "unknown")" : "Target temperature")
                        .font(.system(size: 11)).foregroundStyle(.secondary)
                }
                Spacer(minLength: 0)
                stepButton("plus", title: "Increase temperature", direction: 1)
            }
            if let bounds = store.temperatureBounds, bounds.minimum < bounds.maximum, let target = store.target {
                Slider(value: Binding(get: { draft.value ?? target }, set: { draft.change($0) }), in: bounds.minimum...bounds.maximum, step: bounds.step) { editing in
                    if !editing { draft.commit(store: store) }
                }
                .disabled(!store.can(.temperature)).accessibilityLabel("Target temperature")
                .accessibilityValue((draft.value ?? target).formatted() + " degrees Celsius")
            }
        }
        .padding(.vertical, 6)
        .onDisappear { draft.cancel() }
        .onChange(of: store.mode) { _, _ in draft.cancel() }
        .onChange(of: store.selected?.id) { _, _ in draft.cancel() }
        .onChange(of: store.pending) { _, _ in draft.cancel() }
        .onReceive(NotificationCenter.default.publisher(for: .miraiePanelClosed)) { _ in draft.cancel() }
    }
    private func stepButton(_ symbol: String, title: String, direction: Double) -> some View {
        Button { if let step = store.temperatureBounds?.step { draft.increment(direction * step, store: store) } } label: {
            Image(systemName: symbol).font(.system(size: 15, weight: .medium)).frame(width: 34, height: 34)
                .background(Color.primary.opacity(0.06), in: Circle())
        }.buttonStyle(.plain).disabled(!store.can(.temperature)).accessibilityLabel(title)
    }
}
