# openMirAIe · macOS menu bar

Native MirAIe India AC controls for macOS 14+. A snowflake in the menu bar opens a compact SwiftUI panel: power, room/target temperature, mode, fan, independent display, Eco, Powerful, airflow positions and capacity. Optionally show the reported target beside the icon. The app has no Dock icon, browser runtime, external runtime packages or MCP-server dependency.

## Build and open

Requires macOS and full Xcode with the macOS SDK. From the repository root:

```sh
python3 scripts/project.py build macos
python3 scripts/project.py test macos
```

Open `build/DerivedData/Build/Products/Debug/OpenMirAIeMac.app` inside this directory to launch the development app. It appears in the menu bar. Sign in **inside the native panel**, then explicitly select an AC. Signing into another app or the MCP connector does not sign into this app. Use Settings to change the selected AC, control the menu bar label or sign out. Quit is always available at the bottom of the panel.

Alternatively open `OpenMirAIe.xcodeproj` and run the shared `OpenMirAIeMac` scheme. The project-owned `python3 manage.py build` / `test` helper honors `DEVELOPER_DIR`, otherwise uses the standard full Xcode installation without changing global settings. Builds use local ad hoc signing and retain the app sandbox with outbound network permission. Distribution signing/notarization and installation are separate steps. There are no Swift package dependencies or lockfiles to resolve.

Debug builds also support an explicitly synthetic review mode:

```sh
open build/DerivedData/Build/Products/Debug/OpenMirAIeMac.app --args --preview
```

The preview is labelled, uses synthetic devices and an in-memory transport/session, and never opens Keychain or connects to MirAIe. Close a running instance before switching between preview and real sign-in. Release builds omit the preview implementation.

## Control behavior

- Session tokens use this app’s own non-synchronizing Keychain entry; passwords are discarded after submission and when dismissing the panel. No credentials belong in chat, shell commands or screenshots.
- Discovery preserves homes/spaces/device categories. Select one AC explicitly; the choice persists locally. Only reviewed direct AC model `140013` (CS-CU-NU18BKY5WX) gets controls, intersected with its actual capability metadata. Other models are read-only; partner/parent-routed devices are not given this AC transport.
- A temperature slider applies once on release; plus/minus changes coalesce after 450 ms. Closing the panel, changing rooms/mode or interrupting editing cancels unsent drafts. Ordinary temperature and mode changes need no confirmation.
- Eco confirms its 26°C target effect. Powerful/limited-capacity conflicts ask before applying a verified sequence. Each dependent effect waits for reported readback. Display remains independent of AC power; Dry controls its own fan, and Fan-only hides target temperature.
- Every command reads fresh model/status preflight, sends once, and checks up to three bounded readbacks. Authentication or ambiguous delivery never replays a command. Partial/unconfirmed outcomes preserve reported state and require a read-only refresh before further changes. A cloud match does not prove physical actuation.
- Closing the panel does not cancel an already submitted change. System sleep interrupts it and marks the result unconfirmed. Waking marks old status stale. No command is queued for reconnect or sign-in.
- Refresh is manual, after a command, or when opening stale status (optional). Reports older than 90 seconds are labelled last known and disable controls until refreshed. A local timer updates freshness only; there is no background cloud polling, history collection or analytics.

## Architecture and verification

`OpenMirAIeMacApp.swift` owns AppKit lifecycle/status item/popover, `MenuPanel.swift` owns native SwiftUI controls and cancellable temperature drafts, `ACStore.swift` owns application state, `MiraieClient.swift` owns HTTPS/Keychain/send-once execution, and `MiraieProtocol.swift` owns capability interpretation/guards/plans. Platform-owned protocol code was reviewed against the [portable skill](../../../skills/panasonic-miraie/SKILL.md) and the independent tvOS implementation.

See [testing.md](testing.md) for synthetic native test coverage and remaining live/accessibility acceptance. The protocol has operation-scoped live evidence on one AC, but this Mac app has not yet been verified against a live appliance. There is no claimed support for every MirAIe device or firmware. No live sign-in or appliance writes occur in build/test.
