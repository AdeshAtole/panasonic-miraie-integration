# openMirAIe production apps

The new openMirAIe product has independent native platform implementations. The retained Room AC app is a separate [PoC](../../prototypes/room-ac-android/README.md), not the production app.

| Target | Location | Current stage | Build owner |
|---|---|---|---|
| Android | [android](android/README.md) | Active first iteration | Pinned Gradle project + wrapper |
| iOS | [ios](ios/README.md) | Planned | Future Xcode project + shared scheme |
| macOS | [macos](macos/README.md) | Planned | Future Xcode project + shared scheme |

All platforms follow the [protocol skill](../../skills/panasonic-miraie/SKILL.md) and [interaction contract](../../design/openmiraie/flows/user-flows.md). Shared product behavior does not require identical UI or a common UI runtime. Use platform-native credentials, lifecycle, widgets and accessibility.

Android uses Java 17 and platform-native Views/RemoteViews with no runtime third-party dependencies. Other platforms have no SDK choice implied by these boundaries. Activate each target through `projects.json` only after its real project, dependency locks and meaningful tests exist. Apple clients may later consume a common Swift package under `packages/`; keep shared authentication/session/device logic separate from app UI and platform credential stores.
