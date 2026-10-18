# Room AC · native iOS

Stage: **planned**, not yet buildable.

Create a native Swift/Xcode app here when work starts. The provisional root build adapter expects `RoomAC.xcodeproj`, a committed shared `RoomACiOS` scheme, and an unsigned generic iOS Simulator build. Update the registry to match the actual project if using a workspace or different scheme.

Keep iOS widgets/extensions and entitlements in this project. Use Keychain, native lifecycle/accessibility and an explicit widget refresh policy. Extract a reusable Swift protocol/client package under `packages/` only when there is actual shared logic with macOS; never depend on Android PoC source or credentials.

Commit project configuration and dependency resolutions for any selected packages. Add real XCTest targets, set a concrete available simulator destination for `xcodebuild test`, then register that test command and activate the target in `projects.json`. A generic simulator build destination is not a valid claim that tests ran. Full Xcode on macOS is required; release signing is a separate setup.

`python3 scripts/project.py build ios` currently reports the planned stage. No production build or simulator test is claimed.
