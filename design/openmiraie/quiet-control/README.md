# openMirAIe Quiet Control design handoff

The approved direction is warm white, charcoal and small blue accents, using stock Lucide icons and Material 3 interaction patterns. The rotary controller is the visual reference for the full Android app. This design replaces the lilac controller and the earlier explicit temperature Set interaction. The user approved this direction and authorized native Android implementation on 19 October 2026. The preview remains a design artifact; implementation never implies authorization for live appliance commands.

## Approved controller refinement — 19 October 2026

The user approved [the latest controller](fan-explorations/airflow-scene.html) for native implementation. It supersedes the fan, airflow and utility controls in the older full-app preview: expanding Fan with Auto and a release-to-apply speed slider; illustrated airflow with independent swing buttons and fixed-axis aiming; equal-width Display/Powerful/Eco buttons; and Converti8 as a filled capacity control that collapses when Off. Preserve native capability restrictions, reported/draft/pending separation and coupled-effect confirmations. Horizontal ordering in the scene is illustrative and must not replace the reviewed model adapter.

[Controller interaction notes](fan-explorations/README.md) and [reference hashes](reference-manifest.json) preserve the accepted state. The checkpoint preserves the approved input; [native implementation and emulator verification](native-inline-review.md) now cover these refinements.

## Review files

- The connected review is `quiet-app.html` in this directory. It is the same self-contained fragment shown in chat. `app-extension.js` is its authored full-app design logic, not production code.
- The original approved dial exploration remains in the thread's visualization directory as `quiet-controller.html`.
- Screenshots, screen inventory, journey checks and sampled contrast results are under `artifacts/quiet-controller/` at the repository root.

## Screen families

| Family | Screens and states |
| --- | --- |
| Account entry | Sign-in, inline validation, failed sign-in, password visibility, reset via MirAIe, discovery, no ACs, expired session |
| Rooms | Multi-AC picker, unsupported connection, room loading, independent room state, one-device automatic selection specification |
| Controller | Rotary temperature, power and display, ready/loading/offline/unconfirmed/failure, Off/Fan/Dry/cleaning/unknown capability/unknown display |
| Control sheets | Mode, fan including Quiet, airflow overview, independent vertical and horizontal positions, More, Eco, Powerful, capacity and coupled confirmations |
| Readings | Current report, unavailable fields, snapshots, no saved readings, seven-day energy unloaded/loading/loaded/empty/failed/stale, day detail |
| Settings | Account, sign-out confirmation, system appearance explanation, device details, reported fields, refresh capabilities, diagnostics and save result, About |
| Exports | Reading and energy CSV handoff, cancel, saved and failed-save compositions |
| Widgets | Compact and wide configuration, placement handoff, launcher fallback, home-screen specimen, signed-out/no-room/offline/pending/unconfirmed states |
| Adaptive | Dark controller/settings/energy, narrow core screens, enlarged-text examples, reduced motion |

Three bottom destinations are Control, Readings and Settings. The room header opens the room picker. The controller's readings icon opens Readings. Secondary pages return via Back. Mode choices apply immediately on tap; selecting the current mode closes without a write. Other selection sheets keep local drafts and explicit Set. Android system bars, file picker, launcher and keyboard must remain native.

## Foundations

| Role | Light | Dark |
| --- | --- | --- |
| Canvas | #F7F7F2 | #191C1B |
| Surface | #FFFFFF | #252A27 |
| Text | #202423 | #F1F3EE |
| Secondary text | #656B68 | #AFB8B1 |
| Divider | #E3E5DF | #3D4540 |
| Accent | #2459D3 | #9AB9FF |
| Accent surface | #E8EEFC | #263A5D |

Use Roboto/system Android type, 24 dp horizontal padding (20 on narrow layouts), 8/12/16/20/24/32 spacing, 24 dp cards, 28 dp action pills and 32 dp sheets. Main headings are 28 sp, sheet headings 24 sp, body 16 sp and supporting text 12–14 sp. The controller numeral is approximately 96 sp with a subordinate degree symbol. Do not render the sample status bar as app UI. Long pages are full-content screenshots; native content scrolls independently of system bars and bottom navigation.

Use one stock Lucide family with consistent 1.8–2 stroke weight and optically aligned 20–24 dp icons. Small visible icons retain at least 48 dp interactive bounds. Material 3 supplies the behavioral foundation: bottom sheets, radios, progress, buttons, native text fields and accessibility semantics. The visual preview is not an actual Material component implementation. The rotary control needs a native accessible range implementation; do not imply it is a stock Material knob. Native Views versus Compose is an implementation decision, not a migration performed by this work.

## Temperature interaction

The user's latest decision removes the temperature Set button and ordinary confirmation. Dragging previews whole-degree values locally. Release commits the final value once. A cancelled/interrupted gesture sends nothing. Clamp against the active model's advertised bounds and increment; 16–30 °C and 1 °C are fixtures. The dial spans 270 degrees, has end stops and never wraps maximum to minimum. Drag initiation does not jump to the touched angle.

For keyboard or accessibility increments, coalesce changes over a brief 450 ms quiet period; the web preview also applies on blur, but the native implementation cancels on focus loss, backgrounding and navigation to avoid an interrupted edit issuing a command. Native accessibility must expose a labelled adjustable range with increase/decrease actions and a clear pending announcement. Do not require circular gestures from TalkBack or switch-access users. Native testing must evaluate accidental touches, one-handed use and appropriate haptics.

Keep observed, selected and pending values separate. During drag, show only the selected number. Omit “Set to”, “Release to apply” and “Turn to adjust”. Room temperature appears above the dial, outside the ring. During a request, show the requested value labelled “Applying” with the previous observed value below. Matching cloud readback changes the reported value. Failure/unconfirmed preserves the observed value and offers Refresh; no silent resend, automatic rollback or reconnect queue. A cloud match does not prove physical movement or that the room reached its target. Navigation and conflicting writes are temporarily unavailable in this preview during a request.

## Other control contracts

- Power and display are independent direct toggles. Preserve their reported state while pending. Unknown display state disables its toggle. Power off still allows display when the actual capability permits it.
- Mode applies immediately on tap. Inline fan, fixed-axis airflow aiming and capacity apply once on release; exact-position fallback sheets retain per-axis Set. Fan mode has no target temperature. Dry manages fan. Cleaning blocks temperature, fan, airflow and Powerful as specified by the native guards. Missing/ambiguous capabilities and offline state fail closed.
- Airflow uses the reviewed model 140013 mapping only. Codes are positions, never degrees. Horizontal directions are while facing the AC. Position 1 appeared centered; 2 was left-facing; 3 was slightly left of 1; 4 was right of 3; 5 was the rightmost tested fixed position. The owner later confirmed the left-to-right order 2, 3, 1, 4, 5. Fixedness for 1/2 remains unverified. Vertical 5 was the lowest tested fixed position. Historical observations do not confirm a new physical movement.
- Eco requires Cool. Enabling Eco verifies Eco first, then the 26 °C target. Disabling does not restore the prior target.
- Powerful from fixed capacity verifies Normal capacity first. Fixed capacity with Powerful verifies Powerful off first. Partial completion retains the first confirmed effect. The example capacity choices are Normal, 40/50/60/70/80/90/100/110%; filter against capabilities. JetStream capacity changes remain unavailable until separately verified cancellation via MirAIe.
- Room switching clears the previous device presentation before loading the newly selected device. Device-specific state stays separate. Never show a previous room's report under the new name. Do not carry an unsent gesture across rooms.

## Data and system handoffs

All data in the preview is synthetic. No credentials are submitted, no network is called, no appliance is controlled, and no real CSV, diagnostic file or widget is created. Saved/failed system screens demonstrate outcomes. Actual export requires Android CREATE_DOCUMENT and success only after the write completes; cancellation is a no-op. Widget placement remains launcher-controlled. Diagnostics are redacted and app-private. Password reset uses MirAIe; no reset endpoint or pairing flow is invented.

Energy is on demand, with missing days shown as missing, today marked partial and the total computed from reported entries. The fixture total is 12.9 kWh across six reported dates. Snapshots are per selected AC, recorded through existing read paths, with 5,000 records globally. Do not imply continuous logging. Sign-out removes account session/current values while local snapshots remain, matching existing semantics.

## Motion and accessibility

Use 80 ms press-in / 140 ms release, approximately 240 ms sheet entry with 24 dp travel and an ease-out curve, and restrained state transitions. The dial follows the gesture immediately with discrete value detents; no spinning decoration or inertia. A bounded request indicator may rotate while pending. Reduced motion removes translations and scale and preserves every state label; loading correctness never depends on an animation completing. Native dismissal should restore focus to the launcher.

The preview includes keyboard range control, modal focus containment, accessible control labels and a larger-text layout example. Browser checks do not certify TalkBack, 200% Android sp scaling, switch access, IME avoidance, lifecycle or native performance. Test all of those, 48 dp targets, long/localized room names, narrow widths and landscape before accepting implementation.

## Remaining transfer and acceptance

The connected local/Page preview is the current visual review source. Figma still needs editable frames, component instances, semantic bindings and prototype links when the quota allows work. Preserve the approved controller composition during transfer. Native implementation has begun in `apps/openmiraie/android/`; see [implementation status](implementation-status.md) for the actual scope and verification. Figma completion, device installation, live commands, release and messages to another agent are separate from this local implementation.

## Agent entry point

Use this directory as the source of truth for implementation, before older prototype files. `reference-manifest.json` pins the approved preview and reference screenshots by SHA-256. `reference/` contains the approved controller and full-app overview. The app uses native Views; the HTML is never packaged as its runtime. Read [implementation-status.md](implementation-status.md) before continuing work.

## Controller refinement — 19 October 2026

The latest user feedback replaces the textual mode arrow with the stock Lucide chevron, removes mode confirmation/Set, removes idle and drag instructional copy, and moves room temperature above the ring. Pending temperature still shows Applying and the prior cloud-reported target. Native code and the connected HTML preview reflect this decision; saved reference PNGs predate it. The later approved inline refinement below supersedes the earlier fan/airflow/capacity sheets.

Native margin/alignment refinements and emulator evidence: [19 October layout review](emulator-review.md). These spacing fixes supersede the older reference PNG geometry.

Fan speed alternatives are available in the [interactive HTML exploration](fan-explorations/index.html).
These preserve the alternatives considered before the approved inline implementation. See [exploration notes](fan-explorations/README.md).


### Expanding Fan + Airflow design review

The selected Fan interaction is now extended to Airflow in
[the combined HTML prototype](fan-explorations/expanding-controls.html).
The later airflow scene was approved and implemented natively. Interaction,
readback and capability details are in
[the exploration notes](fan-explorations/README.md#selected-direction-combined-expanding-controls-19-october-2026).


### Photo and draggable airflow concept

[The photo-based scene](fan-explorations/airflow-scene.html) explores a two-axis
drag-to-snap interaction with independent swing switches. It is the approved interaction reference and has been implemented natively; it is not a measured airflow
view. See [its interaction and limitation notes](fan-explorations/README.md#new-concept-photo--two-dimensional-airflow-scene-awaiting-approval).
