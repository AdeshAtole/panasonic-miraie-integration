# Native inline controller review

19 October 2026. Implements the approved [airflow-scene.html](fan-explorations/airflow-scene.html) in the Android app, following checkpoint `70faeaa`. The HTML remains the saved design input. This report describes native implementation and synthetic emulator verification, not appliance acceptance.

## Implemented behavior

- Fan expands inline: Auto is a button; supported manual speeds use a native slider. A gesture submits once on release, with no Set step. Keyboard/accessibility increments coalesce for 450 ms.
- Airflow expands into the representative AC photograph and parallel animated streams ending in a horizontal aiming bar. Dragging snaps each fixed axis to supported presets. Either fixed axis remains adjustable while the other swings; both swinging disables manual aiming. Vertical and Horizontal are compact selected-state buttons under Swing.
- Stopping swing restores the last observed fixed preset for that room. If no fixed preset is known, the reviewed position picker opens. The app cannot freeze a vane at a measured physical angle. Existing exact-position sheets remain available through the position-options icon.
- Display, Powerful and Eco are equal-width buttons. Display remains independent. Existing Eco/Powerful/capacity coupled-effect confirmations and readback checks are preserved.
- Converti8 is one filled surface with capacity stops inside it. Off collapses the surface; tapping opens choices without sending. Release applies one supported preset. Returning to reported Off collapses it again.
- Drafts, reported values and pending targets remain separate. Pending airflow has a dashed target outline; pending capacity retains reported fill with a dashed requested boundary and Applying label. Errors/unconfirmed outcomes preserve the latest reported state.
- Cancellation, extra pointers, focus loss, navigation and backgrounding discard unsent edits. A two-axis request validates both targets before the first write, verifies each axis separately and stops on partial failure or changed state. No retry/reconnect replay is introduced.

## Native presentation and adaptation

Platform Views and Canvas, existing semantic colors, stock Lucide icons, and the saved generated AC photograph are reused. No WebView or runtime library was added. The photograph is bundled as a density-independent bitmap; its provenance is recorded in Android `third-party/ac-scene/`.

Panel entry uses 180 ms fade/translation when Android animators are enabled. Air particles move through parallel curves; oscillation is illustrative, not live angle telemetry. System-disabled animation removes particle travel, oscillation and entry translation. Preset detents use system-respecting haptics. Reduced-motion code paths were reviewed; a full native reduced-motion recording remains outstanding.

At 200% text size, utility and swing buttons stack at equal full width. Converti8 keeps its current percentage and endpoint labels, replacing intermediate numeric labels with detents to avoid crowding. Pending capacity places Applying on a separate line at enlarged text sizes; this was corrected after screenshot inspection caught crowding that generic View geometry checks could not detect. Controls retain at least 48 dp touch bounds even where the visible chip is slimmer.

## Verification

- Debug/review/instrumentation APK builds passed.
- All **85 JVM tests passed**, including 14 new discrete-selection and airflow-plan tests. These cover cancellation/no-op consumption, Auto-to-first-manual selection, supported targets, stale state, offline restrictions and prevalidation of both axes.
- Lint: zero errors, four existing warnings (tooling/target versions and localization).
- Offline Android 15 emulator instrumentation passed at approximately 411 dp, light theme, normal text; and 320 dp, dark theme, 200% text. Standard configuration was restored after testing.
- Native interaction checks cover fan adjustment, aiming while the other axis swings, restoring a fixed preset, cancelled drag, both-swing blocking, capacity expansion/fill/collapse, and Powerful/Converti8 confirmation transitions. Deterministic pending-state captures verify reported values remain unchanged while requests are shown.
- Layout checks cover 48 dp enabled touch targets, text height, children escaping clipping containers and status-bar insets. Representative screenshots were inspected with layout bounds hidden. Scroll screenshots intentionally show partial content at the viewport edge; content remains reachable by scrolling.

Local synthetic captures and geometry findings are saved under `artifacts/quiet-controller/native-inline/`. These are review outputs rather than production resources. The review package has no INTERNET permission, uses an in-memory fixture repository and never reads an account. No live appliance command was issued.

## Remaining acceptance

Physical-phone feel, TalkBack/switch-access announcements and focus retention across repository-driven view rebuilds still need hands-on review. Animation frame timing and reduced-motion behavior need a recording-based device pass. Broader API/OEM, rotation/process recreation and real readback/error transitions are not certified by this emulator run.

Horizontal preset geometry is uncalibrated. The owner subsequently confirmed the left-to-right order 2, 3, 1, 4, 5; the native scene and current HTML reference now use that order. Vane angles and fixedness of previously unverified positions remain unverified. The reviewed model adapter and position descriptions remain authoritative. Cloud readback is not physical confirmation.

## Scroll and refresh follow-up

Setting updates now retain the viewport through rapid pending/readback renders. Position is restored after layout, avoiding the reproduced reset-to-zero race. Pull down at the top of Control, Readings or Energy for a read-only refresh. A compact native indicator overlays the content without changing its layout. Short/cancelled/horizontal pulls and attempts during another request do not refresh. The accessible Refresh action invokes the same guarded operation. Native regression checks reproduce the original failure and cover both pending and completed setting changes.

Follow-up validation: debug/review builds, 85 JVM tests, lint (zero errors), repository checks, focused native scroll/refresh regressions and the complete offline emulator layout/interaction audit passed. The pending-refresh indicator was inspected in an emulator screenshot.
