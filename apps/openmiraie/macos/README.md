# openMirAIe · native macOS

Stage: **planned**, not yet buildable.

Create a native Swift/Xcode app here when work starts. The provisional root build adapter expects `OpenMirAIe.xcodeproj`, a committed shared `OpenMirAIeMac` scheme and an unsigned macOS development build. Update the registry if the real project uses a workspace or other names.

Own native windows/menu-bar UI, widgets/extensions, entitlements, sandbox/network access, Keychain, accessibility and lifecycle here. A future shared Swift package may implement common MirAIe behavior with iOS; platform UI and credential-store adapters remain native.

Commit actual project/dependency configuration, add XCTest targets and their real test command, mark the target active in `projects.json`, and add macOS CI. Signing, notarization and distribution are separate from the unsigned build adapter.

`python3 scripts/project.py build macos` currently reports the planned stage. No production code or verified build is claimed.
