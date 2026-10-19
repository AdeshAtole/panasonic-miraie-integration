# openMirAIe · native Android

Stage: **active first iteration**, version 0.1.0. Package `in.openmiraie.android` is separate from the installed Room AC PoC (`in.room.ac`). No PoC source, prototype HTML, credentials or signing files are imported. This is a buildable implementation with partial Android 16 device validation; remaining acceptance gaps are listed below.

## Stack and build

Java 17, native Android Views and RemoteViews, platform HTTPS, Android Keystore AES-GCM and private SQLite. No runtime third-party libraries, WebView, JavaScript engine or cross-platform runtime. Application-scoped serialized work survives activity recreation; commands are never persisted or replayed. Platform dialogs supply modal focus, Back and cancellation behavior. The light/dark theme follows the approved [Quiet Control handoff](../../../design/openmiraie/quiet-control/README.md): warm white, charcoal and small blue accents. Stock Lucide 0.468.0 vectors are vendored with license/provenance in `third-party/lucide/`; there is no icon runtime dependency. Material interaction conventions are implemented with platform Views, not the Material 3 component library.

Pins: Gradle 8.13 (wrapper JAR and distribution SHA-256), Android Gradle Plugin 8.10.1, compile/target SDK 35, build-tools 35.0.0, minimum Android 26, Java toolchain 17. Test-only JUnit 4.13.2 and JSON 20240303 are locked. Gradle dependency verification includes macOS and Linux AAPT2. Verification hashes were generated from resolved build artifacts; this is integrity pinning, not an independent security audit.

Set `JAVA_HOME` to a JDK 17 installation and `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) to your SDK. Install platform 35 and build-tools 35.0.0 explicitly. No developer paths are stored in the project. The first Gradle invocation resolves pinned tooling/test dependencies and may require network access; use `--offline` after cache provisioning. Gradle does not download a Java toolchain automatically.

From the repository root:

```sh
python3 scripts/project.py doctor android
python3 scripts/project.py build android
python3 scripts/project.py test android
```

From this directory:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (ignored). Reports: `app/build/reports/tests/testDebugUnitTest/` and `app/build/reports/lint-results-debug.html`. Debug signing is independent of the preserved PoC build/signing files. Release signing, installation and distribution are deliberately separate tasks. The Android CI job builds/tests/lints on Linux; it does not install, authenticate or upload APKs.

## Implemented flows

- Email/mobile sign-in, transient password, show/hide, autofill, inline validation, discovery across all homes/spaces, automatic selection of one supported AC, and a cancellable picker for multiple ACs. Unsupported categories/partner transports remain visible but cannot be controlled. Password reset is handed back to MirAIe.
- Quiet Control controller with native 270-degree rotary temperature input, capability-derived increments and one submission on release. Keyboard/accessibility adjustments coalesce for 450ms; cancellation, focus loss, navigation and backgrounding discard unsent edits. Pending temperature is separate from observed state and survives activity recreation through the repository. Direct power/display buttons remain independent. Mode applies immediately on tap. Fan expands inline with Auto and a release-to-apply speed slider. Illustrated airflow supports fixed-axis drag-to-snap while the other axis swings. Display/Powerful/Eco are equal-width compact buttons; Converti8 is a filled capacity surface, collapsed when Off. Fan, aiming and capacity submit once on release; Eco/Powerful coupled-effect confirmations remain. Control, Readings and Settings form the bottom navigation; Energy and Saved Readings have separate destinations.
- Capability-derived wire keys, ranges and choices. AC builders bind reviewed capability names to registry indexes. Dry never forces Low; Fan hides target; power/cleaning/mode gates apply. No invented timer, cleaning or sensor controls. Swing follows the separately reviewed model-scoped adapter below.
- Independent vertical/horizontal swing sheets for direct model 140013, advertised integer codes only, separate observed/draft values and explicit per-axis Set. Descriptions preserve the corrected owner observations, facing-the-AC perspective and unverified angles; they never imply a new physical confirmation.
- Send-once mutations, five bounded readback waits (swing waits three seconds before the first read, then 1.2/1.8/2.4/3 seconds), no network retry of a mutation, step-by-step coupled verification and partial/unconfirmed messages. A fresh read before writing rechecks guards. If mode or coupled state changed since confirmation, the action stops for review.
- Read-only current fields and sorted advanced details, on-demand seven-day Daily energy with decimal totals/today partial, selected-device snapshot count, 5,000-record global SQLite retention, separate reading/energy CSV through the system document picker, and redacted app-private diagnostics.
- Compact and wide launcher widgets following the selected AC, manual refresh, explicit power and independent wide display actions. One-shot widget jobs expire after 15 seconds, are not persisted/retried, and reject changed device/state. Launcher pin support has a manual placement fallback.
- Keystore-encrypted session, one serialized refresh for reads, account-state clearing on expiry, no command replay after sign-in. Backup and device-transfer exclusions cover session, snapshots and diagnostics. Local snapshots intentionally remain after sign-out.

## Boundaries and remaining validation

The protocol reference is MirAIe India 01.4.32 and the previously verified CS-CU-NU18BKY5WX/model140013 behavior. Partial live validation was performed on one USB-connected Android 16 phone on 19 October 2026: saved-session loading after app update, discovery/controller reads, manual refresh, seven-day energy, reading/energy CSV saves through the system picker, picker cancellation, temperature draft/tab/rotation preservation, and compact/wide widget placement and refresh. A 22°C target request and restoration to 21°C received matching cloud readback; display Off also received matching readback. A Low fan request remained unconfirmed. Concurrent user automation was then identified, so subsequent actuation tests were stopped and its chosen 26°C target was left in place. These checks do not establish physical effects or isolated fan/control acceptance. Unsupported transports, ambiguous capability shapes and missing online/state fields disable writes. Heat is outside this initial reviewed adapter. Active JetStream cancellation is not implemented. No cleaning command or sleep-profile reset is invented.

The native presentation now follows Quiet Control, replacing the lilac hero, temperature stepper/Set, paired switches and segmented modes. Sign-in, rooms, readings, settings, sheets and widgets share the new resources. The redesigned app was installed and sampled on the connected Android 16 phone: navigation, power-off restrictions, light/dark, enlarged text and Settings rotation. Airflow label wrapping, cross-page scroll leakage and oversized degree glyph were corrected and the updated app installed. Full visual/accessibility and enabled-dial acceptance remain open; see the [device review](../../../design/openmiraie/quiet-control/device-review.md). Earlier actuation checks above describe the previous UI. Native bottom sheets use a 240ms entry when system animators are enabled; dial motion directly follows input, with system-respecting step haptics. See the [implementation status](../../../design/openmiraie/quiet-control/implementation-status.md) for remaining visual/interaction gaps. English strings are not yet fully extracted for localization. Widget gestures remain send-once and never replay after restart.

Synthetic tests cover capability parsing, missing/offline/cleaning states, coupled sequencing/partial failures, one-send bounds, discovery, redaction, decimal energy and CSV escaping. They do not validate Keystore, SQLite retention, actual file-provider behavior or launcher rendering. The initial implementation kickoff used synthetic checks only; the later authorized Android 16 session exercised the partial live checks above. No Android 26/35 device coverage or full accessibility acceptance is claimed.

Before full device acceptance, finish an isolated control run without concurrent automation, TalkBack and resized widget checks, then verify on Android 26 and 35: sign-in/expiry/rotation/process recreation; keyboard and autofill; picker cancellation and switching; drafts and partial failures; file-picker cancellation/write failure; light/dark, 200% text, TalkBack and 48dp controls; system Back/insets; compact/wide widgets at minimum/resized bounds and after restart/sign-out. Test SQLite cap/filtering and unavailable storage. Then separately authorize reversible live checks and distinguish cloud readback from physical observation.

## Swing support and evidence

The native `Swing` adapter binds the selected direct AC and model metadata to
VerticalSwing/index 5 and HorizontalSwing/index 6. Keys come from each actual
attribute; integer lists/ranges/steps may narrow the reviewed 0–5 codes. Missing,
conflicting, duplicate, unit-bearing or unsupported metadata disables writes.
Online state, power On, cleaning explicitly Off, a known current code and no
pending mode/request are required. Partner/child routes and other models cannot
write swing. Each axis is sent independently through the existing native repository. A two-axis gesture validates both targets first, then verifies each sequentially, stopping on partial/unconfirmed outcomes;
there is no MCP runtime dependency or reconnect command queue.

Use the [corrected swing reference](../../../skills/panasonic-miraie/references/swing.md)
for evidence: both axes code 0 oscillates; vertical 5 was the lowest tested fixed
position. Horizontal 1 appeared centered; 3 moved slightly left of 1. Left/right
is while facing the AC. The owner later confirmed the left-to-right order 2, 3, 1, 4, 5. Fixedness at 1/2,
calibrated angles and other firmware remain unknown. A successful app command means a
matching cloud code, with physical movement explicitly unconfirmed.

See [Android testing and support status](testing.md) for synthetic coverage,
native checks and the remaining separately authorized appliance acceptance.

## Offline UI review

`./gradlew :app:assembleReview :app:assembleReviewAndroidTest` builds a separate
`in.openmiraie.android.review` app with synthetic rooms and no Internet permission.
It uses the production native screens, including mode, dial and airflow controls.
See [testing.md](testing.md#offline-emulator-layout-review) for installation,
layout-bounds inspection and the platform-only geometry audit. Review fixtures
and instrumentation do not ship in debug/release. The production app remains
account-backed; never use a real phone/account for synthetic layout automation.

## Inline controller refinement — 19 October 2026

The approved [airflow scene](../../../design/openmiraie/quiet-control/fan-explorations/airflow-scene.html) is implemented natively. See [implementation and emulator evidence](../../../design/openmiraie/quiet-control/native-inline-review.md). The photograph and animated stream are illustrative, not a camera feed or measured vane angles. Exact-position sheets retain reviewed model descriptions. Stopping either swing axis applies its last known supported fixed preset directly, without a picker. With no usable history, it selects position 1 when supported, otherwise the first advertised fixed preset. This does not claim to freeze at a measured physical angle. Pending airflow/capacity targets are drawn separately from reported state.

## Scroll stability and pull to refresh

Repository updates preserve each page’s scroll offset after layout, including rapid pending/readback renders. Pull down at the top of Control, Readings or Energy to refresh that page’s data; short, cancelled and horizontal gestures do not refresh. An active request blocks another refresh. The gesture respects child controls that own dragging, and an accessibility Refresh action provides an alternative. Refresh reads data only; it does not resend a setting.
