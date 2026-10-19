# Android support and testing

## Swing adapter — 19 October 2026

Scope: direct MirAIe India AC model 140013 / CS-CU-NU18BKY5WX. The native app
uses its own HTTPS/session/domain path. It has no runtime dependency on MCP.
The [portable swing reference](../../../skills/panasonic-miraie/references/swing.md)
owns the corrected model-scoped protocol and owner-observation evidence.

### Synthetic verification

The debug build, 59 JVM tests (37 existing + 22 swing tests), and lint pass.
Lint has zero errors and two existing tooling/target-version warnings.
The swing tests exercise:

- Both axes, every code 0–5, exact integer wire type, actual attribute keys,
  native indexes 5/6, common envelope and independent-axis payloads.
- Narrower ranges and steps, explicit integer lists, singleton range shapes,
  missing/malformed/conflicting metadata, duplicate axes, reserved/colliding
  keys, unverified models, parent/partner routes and unsupported categories.
- Online/power/cleaning guards, unknown or unsupported current codes, and
  availability in supported AC modes without inventing mode restrictions.
- One send, first read after 3,000 ms, at most five reads, no send/read retry on
  errors/expiry, accepted-but-unconfirmed outcomes, and unchanged observed
  values when a request cannot be confirmed. Unsafe readback ends verification.
- Separate API acceptance, matching cloud report and physical confirmation.
  The command runner never asserts physical confirmation.
- The same presentation-state function used by the native sheets: drafts do
  not replace reported codes, the opposite axis remains independent, unchanged
  or invalid selections cannot apply, pending mode/request states disable Set,
  and unknown/missing readings remain unavailable.
- Corrected labels and observer perspective. Horizontal 3 is not labeled
  center; code 2 is not labeled leftmost. Position meanings remain scoped.

Run from this directory with the documented JDK/SDK:

```sh
./gradlew --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

### Native and live acceptance

Read-only Android 16 checks passed: the controller shows both current codes;
both axis sheets load the advertised 0–5 choices; a selection enables Set while
leaving the reported code unchanged; vertical draft 5 survives portrait/landscape/
portrait recreation; Back discards each draft; reopening shows the unchanged
reported codes. Horizontal 3 displays the corrected slightly-left-of-1 label.
The vertical sheet was visually inspected. PiP remained active and rotation
was restored. Neither Set button was pressed.
No Android swing command or physical movement has been tested in this pass.
The existing MCP evidence is not an Android actuation sign-off.

Before Android swing acceptance, separately authorize an isolated live session,
read the actual baseline freshly, pause competing controllers, and verify one
axis at a time with owner observation and baseline restoration. Do not assume
historical 21°C/display On/horizontal 1/vertical 1 is still current.

Finish TalkBack and large-text sheet checks, rotation/process recreation,
session expiry, changed capabilities while a draft is open, missing/offline
readback, and Android 26/35 coverage. Sheets retain only local UI selections
across safe configuration recreation; no pending command is stored or replayed.
On sign-out or device change the sheet/draft is discarded. A widget launch opens
Controller rather than reopening a saved swing sheet.

### What the UI means

`AC reports` is the latest observed code. `Selected` is a local draft. Set is
explicit and per-axis. A successful HTTP send is acceptance; matching cloud
readback is reported confirmation. Historical owner descriptions in the sheet
are context, never new physical confirmation. Code 0 oscillates on both axes.
Vertical 5 is the lowest tested fixed code. Horizontal left/right is while
facing the AC: 1 appeared centered; 2 was left-facing; 3 moved slightly left
from 1; 4 moved right from 3; 5 moved farther right and stayed fixed. Ordering
of 2 versus 3, exact angles, fixedness at 1/2 and other firmware remain unknown.

## Quiet Control phone review — 19 October 2026

The redesigned debug build was installed with user authorization. Sampled UI checks and three presentation fixes are recorded in the [device review](../../../design/openmiraie/quiet-control/device-review.md). Build, 71 JVM tests and lint pass (zero errors, four warnings). No appliance write was performed. The enabled dial, TalkBack and full native acceptance remain open.

## Offline emulator layout review

The `review` build type reuses production Views with an in-memory synthetic
repository. Its package is `in.openmiraie.android.review`, its label includes
Review, and its merged manifest **removes INTERNET permission**. It never loads
an account or sends appliance requests. Review classes are absent from debug
and release. Do not add fixtures or review entry points to production builds.

Build the app and platform-only instrumentation APK:

```sh
./gradlew --no-daemon :app:assembleReview :app:assembleReviewAndroidTest
adb -s <emulator-serial> install -r app/build/outputs/apk/review/app-review.apk
adb -s <emulator-serial> install -r app/build/outputs/apk/androidTest/review/app-review-androidTest.apk
adb -s <emulator-serial> shell am instrument -w in.openmiraie.android.review.test/in.openmiraie.android.LayoutReviewTest
```

The custom instrumentation runner reports `Layout audit passed` only when all
checks pass. Do not treat adb's process exit code alone as test success. It
asserts the separate package and absence of network permission, opens the real
screens/sheets, checks enabled touch targets of at least 48 dp, text height,
children escaping clipping containers, and status-bar insets. Scrollable content
is inspected at top/bottom. Actual screenshots, findings and configuration are
saved under the review app's external `files/layout-review/` directory. Retrieve
with `adb -s <emulator-serial> pull /sdcard/Android/data/in.openmiraie.android.review/files/layout-review`.

Use Developer options → Show layout bounds (or `adb shell setprop debug.layout
true`, then restart only the review app) to inspect view edges. Hide bounds for
visual polish checks. Scope every adb command to the intended emulator, especially
when a real phone is connected. This runner owns `testBuildType = "review"`;
run it directly as above, not through a JUnit/AndroidX connected-test reporter.

See the [layout review](../../../design/openmiraie/quiet-control/emulator-review.md)
for inspected configurations, corrected defects and remaining acceptance gaps.
