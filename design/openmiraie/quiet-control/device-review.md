# Quiet Control — connected-phone review

19 October 2026. User authorized phone testing and starting the existing silent-video PiP helper. The updated debug app was installed over the existing app, preserving its saved session. No sign-in credentials were handled and no appliance-changing control was used.

## Verified on the connected Android 16 phone

- App update, launch and saved-session readback succeed.
- Controller, Readings, Energy entry and Settings open. Controller and Settings have independently preserved scroll positions.
- Light/dark appearance and 200% text were exercised. Enlarged-text controller cards stack and Settings rows wrap. These sampled checks are not full accessibility acceptance.
- With the AC reporting power off, temperature, mode, fan and vertical airflow selections/Set remain disabled; the independently reported display control remains available. Airflow overview and vertical sheet open and close with Back without a mutation.
- Settings survives portrait/landscape/portrait recreation. This does not constitute controller landscape acceptance.
- Existing silent-video PiP remains active during app updates, appearance/text changes and rotation. Android reports its VideoActivity as the hold-screen window and the phone as Awake after more than five minutes of testing.
- Original preferences restored: dark mode, font scale 1.0, locked portrait rotation. The app was left on Controller and PiP was left running as requested. Screen-timeout and device-admin policies were not changed.

## Fixed and rechecked

- Airflow card value changed from wrapping “Direction” to “Adjust”; both cards now align at the tested portrait width.
- Each destination stores its own scroll position. Settings no longer inherits a scrolled controller position or opens with its selected-device card cut off.
- The temperature degree symbol now uses subordinate sizing beside the numeral.

The corrected build was installed and the label/scroll fixes rechecked. Build, all 71 JVM tests, lint (zero errors, four warnings), repository checks and diff whitespace checks pass. Private screenshots and UI hierarchies remain in ignored local `analysis/quiet-native-device/`; they are not part of a public design package. Some early captures caught transitions and are not acceptance evidence; UI hierarchies and final settled captures support the results above.

## Not yet accepted

No live temperature, power, display, mode, fan, airflow, Eco or capacity write was attempted. The AC was already off, so an enabled rotary gesture and its pending/readback path remain untested on hardware. TalkBack/switch access, physical keyboard, controller landscape, widget resizing, system export failure/cancellation, process-death recovery and Android 26/35 coverage remain outstanding. PiP obscures part of the screen; full visual acceptance needs an unobstructed pass. Existing supporting-screen refinements remain listed in implementation-status.md.

Close the PiP window or use Stay Awake → Restore normal sleep to stop keeping the display awake.

## Follow-up controller refinement

The user requested a stock mode chevron, immediate mode selection, removal of idle/drag instructions, and room temperature above the ring. The rebuilt app was installed; the new controller was visually inspected and the mode sheet contains only choices and Close, with no Set action. Selecting the current Cool mode dismisses without a command. A different mode was not selected in this review. All 71 JVM tests and lint passed. Silent PiP was restarted on explicit request and confirmed as the hold-screen window.
