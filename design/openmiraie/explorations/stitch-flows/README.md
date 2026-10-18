# Room AC remaining-flow design boards

Three supplemental Stitch boards complete the visual coverage for the existing controller/widget concept: Readings and data export, secondary AC controls, and sign-in/device/state handling. Use the corrected local specimens below for review. The complete interactive flow/spec lives separately under `../../prototype/` and `../../flows/`; these static boards support it and do not claim every illustrated button is connected to an AC.

## Review images

| Area | Main review | Readable details |
|---|---|---|
| Readings, daily energy, snapshots, exports and details | [Viewport](readings-corrected.png) · [Full height](readings-corrected-full.png) | [HTML](readings-corrected.html) |
| Secondary controls | [Full board](secondary-controls-corrected-full.png) | [Fan draft](fan-sheet.png), [More controls](more-controls-sheet.png), [Capacity at80% draft](capacity-sheet-selected.png), [Eco26°C confirmation](eco-confirmation.png), [Command feedback](command-feedback.png) |
| Entry, device and state flow | [Full board](auth-and-states-corrected-full.png) | [Sign in](sign-in.png), [Device picker](device-picker.png), [Mode states](mode-state-board.png), [Pending/offline/unconfirmed](feedback-state-board.png), [Management](management-sheet.png) |

Readings and all local board renders use390logical-pixel width at2×rendering. Capacity choices are internally scrollable; `capacity-sheet.png` shows the top and `capacity-sheet-selected.png` shows the80% draft. All advertised choices are present in HTML. Generated originals, exact prompts, screen IDs and AI descriptions/suggestions are retained for provenance; raw tool exchanges remain in ignored .stitch/archive/flow-stitch; `manifest.json` maps them.

## Corrections and boundaries

The live/generated Readings original incorrectly claimed local flash/LAN/socket transport, guessed raw keys and a diagnostic-log export. Its corrected local copy uses cloud-reported observations, verified example keys (`rmtmp`, `actmp`, `acfs`, `ps`, `acdc`, `rssi`, `errors`), and the existing capability-refresh/diagnostic-collection actions. Diagnostics currently collect a redacted app-local JSON file; this design does not promise a new SAF diagnostic-export API. Current-day examples and a synthetic2026CSV filename were corrected too.

The control-board copy removed unverified certification/maximum-output/inverter-modulation claims and describes a capacity **setting**, not a measured energy-saving percentage. The entry/state board removed the erroneous “Auto only in Dry” claim: Dry disables manual fan selection and lets the AC manage it; no fixed fan readback is promised. The exact dependencies remain EcoCool-only with26°C on enable, PowerfulCool-only and unavailable during cleaning, and Powerful/fixed-capacity mutual exclusion. Capacity is gated by power; it is not incorrectly restricted to Cool by this board.

All examples are synthetic. Missing readings are unavailable rather than zero; offline preserves any last-known readings and timestamp. If no room observation has ever been obtained, an em dash is appropriate. Pending/unconfirmed are separate from success, and actual command success requires readback. Daily energy is on-demandkWh, todaypartial; snapshot and energyCSV exports are separate. No periodic logger, direct local control, schedules, timers, swing, Heat, measuredRPM or humidity are introduced.

Board annotations describe behavior and are not intended as everyday product copy. Native implementation still needs readable contrast/disabled states, TalkBack, text scaling, system insets, keyboard behavior and real command/export testing. These browser-rendered specimens do not establish native correctness. Additional password visibility/confirmation affordances are UI proposals using the existing auth/control paths, not new account APIs.

## Stitch provenance

Existing project: https://stitch.withgoogle.com/projects/16031458568887977513 . Approved design system verified before generation: `assets/64d81fe863ee49ee9985ac1c09bd4b98`. Exactly three serial generation calls were made; no follow-up generation/edit or new design system was created. Each generation's text description and unaccepted suggestions are in its `*-insights.json` file. Suggestions were not treated as authorization to create dark screens or additional flows.

Original remote assets are preserved and may still contain the noted issues. The corrected files are clearly local design artifacts; no claim is made that these local text corrections were persisted to Stitch. No production Java, manifest, API/session, phone, signing material, commit or push changes were made.
