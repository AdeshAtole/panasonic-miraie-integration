# Fan speed interaction explorations

Open `index.html` directly in a browser. It is self-contained, uses embedded
stock Lucide vectors and makes no network requests. The adjacent ISC license
covers their original path geometry. This is a local design comparison, not
an approved replacement for production fan behavior.

The three views are available together or individually with query parameters:
`?view=picker`, `?view=inline`, `?view=expand`.

- Compact picker: open Fan, select one of five rows. Applies and closes.
- Always visible: direct choice among Auto/Quiet and Low/Medium/High. The dial
  is deliberately smaller to make room for these controls.
- Expanding card: opens in place, scrolls only its controller content enough to
  reveal all choices, then collapses on selection.

All three apply the selection without Set. Choosing the already reported speed
is a no-op. Pending retains the previous report; simulated matching readback
updates it after one second. Unconfirmed keeps the old speed and exposes Refresh;
this reads the same sample state and never resends. AC mode Dry disables fan.
The outer comparison toolbar changes sample response, appearance and phone width.
Other controller controls/navigation are static visual context. Nothing connects
to an AC or sends credentials, and no native implementation was changed.

Keyboard focus is retained during pending requests. The picker traps Tab while
open and closes on Escape, backdrop or close button. The expanding card also
closes on Escape. Reduced motion removes motion while keeping status text.
The visible selected state denotes the reported speed, not the pending request.
This is browser validation, not Android/TalkBack acceptance. Haptics are intended
for native implementation and are not simulated in HTML.

Browser review: compact selection closes immediately and shows pending while
retaining Auto; manual Medium updates after matching sample readback; an
unconfirmed High preserves Auto and offers Refresh; expansion selection
collapses; Dry disables all choices; dark appearance works; 320 px preview has
no horizontal overflow; Escape restores picker focus to Fan. JavaScript syntax
check passed. Reduced-motion CSS is implemented but OS preference switching was
not exercised. Larger browser/device coverage remains outside this concept pass.


## Selected direction: combined expanding controls (19 October 2026)

Historical button-layout snapshot: `expanding-controls-positions.html`. The user selected the expanding Fan concept
and requested the same treatment for Airflow in the design. **Approval of this
combined design is pending. Do not implement it in the Android app yet.**

- Only one card expands at a time; the other becomes a compact full-width row.
  Fan selection applies directly and folds back. Same-value selection is a no-op.
- Airflow uses separate Vertical and Horizontal tabs, each showing its last
  reported value. Direct selection applies only that axis. Airflow remains open
  for the second adjustment; its header or Escape closes it.
- The selected choice always reflects readback. During the 1.1-second sample
  request, all write choices are disabled, but axis browsing and collapse remain
  available. Matching readback changes only the requested field. Failure keeps
  the previous report and offers Refresh, which never resends a command.
- Airflow position descriptions come from the reviewed model-140013 adapter in
  `Swing.java`, codes 0–5. Horizontal choices are not drawn as calibrated angles
  or a known left-to-right sequence. Uncertainty details are behind a disclosure.
- Dry blocks manual Fan, not Airflow. Offline, off and cleaning block both.
  Unsupported/unknown airflow blocks Airflow only. These are sample states,
  not a replacement for production capability and preflight checks.
- Expansion uses 240 ms easing, control feedback 140 ms. The controller scrolls
  enough to expose the expanded card. Reduced-motion preferences remove these
  animations and smooth scrolling. No haptics are simulated.
- Touch choices are at least 48 px. Axis tabs support Left/Right, Home/End;
  Escape restores focus to the card header. Inactive/closed choices are excluded
  from keyboard navigation. Large text changes positions to a single column.
- All unrelated controller elements remain static visual context. Local sample
  state resets on reload. No AC commands, backend integration, native changes,
  or Figma sync are included in this review artifact.

Browser review verified direct Fan selection/collapse, independent vertical
readback, horizontal failure retaining Oscillate, and Dry/offline/unsupported
restrictions, power-off/cleaning/unknown guards, axis arrow navigation and Escape focus restoration. Focus rings are inset to avoid clipping against the card. The 320 px large-text dark preview has no horizontal overflow.
JavaScript syntax and whitespace checks passed. Native screen reader, haptics,
physical airflow and OS reduced-motion acceptance remain for implementation.


## Previous revision: numbered stepped sliders

Snapshot: `expanding-controls-numbered-sliders.html`. This superseded the button choices in the section
above, following the user's request for fewer captions and slider controls.

- Fan: separate Auto button; four manual stops Quiet / Low / Medium / High.
  Only Quiet and High appear at the endpoints; the active stop is named above.
  Quiet is a named preset, not a calibrated point on a continuous speed scale.
- Airflow: separate Oscillate button per axis; five numbered preset stops.
  Only the selected stop's reviewed description is shown. Horizontal codes are
  deliberately not labeled as a monotonic left-to-right angle scale. Their
  physical ordering still needs verification before such directional labeling.
- Auto/Oscillate are explicit selections, not ambiguous on/off toggles. Moving
  the slider selects manual control. Pressing an already active preset is a no-op.
  No manual thumb is shown while Auto/Oscillate is active, except a faint keyboard
  focus hint. The last confirmed manual stop is remembered within the session.
- Pointer drag previews locally; release commits one sample request. Keyboard
  arrow/Home/End changes commit on key release; Escape cancels the draft and
  closes the card. Both cards remain open after changes for further adjustment.
- During pending, a hollow thumb and spinner show the requested value. Card
  headers, axis summaries and mode-button selections retain reported values.
  Matching readback updates the report; failure restores the reported slider
  state. Controls cannot issue another request while one is pending.
- Native range inputs have semantic names, value text, stepped keyboard control,
  48 px interaction height, visible focus, reduced-motion support and the same
  capability guards as the previous concept. These are browser design mechanics,
  not Android implementation or TalkBack acceptance.

Verified browser interactions: pointer tap from Auto to Medium; keyboard High
failure restores Medium; independent vertical slider selection; source syntax
and whitespace checks. Horizontal changes left Vertical unchanged; the 320 px large-text dark preview had no horizontal overflow; Dry kept Airflow available while blocking Fan. No app files or appliance state were changed.


## Previous revision: visual airflow sliders

Snapshot: `expanding-controls-visual-sliders.html`. Fan is unchanged from the stepped-slider revision.
Airflow now uses schematic AC illustrations with vane icons instead of visible
numbers. Vertical uses a genuine 48 × 192 px vertical range control; its five
stops run downward. Horizontal retains a horizontal range control. A selected
marker and schematic path highlight together; Oscillate clears the manual
highlight. Axis summaries use the reported vane icon. Accessible names and value
text retain the underlying position number and description.

The illustrations are custom explanatory SVG diagrams, not stock Lucide icons
or measured angle drawings. App chrome still uses the existing Lucide assets.
Horizontal codes remain in their reviewed order; they are NOT remapped into an
assumed left-to-right sequence. The fourth icon depicts movement to the right
rather than claiming a known absolute angle. All drawn rays are schematic;
physical ordering and angle calibration remain unverified. Do not lift the
illustration geometry into production actuator mappings.

Down/Up keys move the vertical selection down/up respectively, with commit on
key release. A pointer drag from the second to fourth marker was verified to
produce one pending Position 4 request while retaining Position 2 in the reported
summary. Horizontal selection remains independent. Existing pending/readback,
mode guards and release-to-apply behavior are retained. Native implementation
still requires user approval; no app or appliance changes were made.

Visual review: the 320 px large-text vertical view has no horizontal overflow;
the native vertical range reports vertical orientation and a 48 px touch width.
Switching axes now brings the card header back into view when the card shrinks.
Screenshots: `vertical-visual-preview.png`, `horizontal-visual-preview.png`.


## Current revision: tap an airflow line (awaiting approval)

Open `expanding-controls.html`. Airflow sliders, below-diagram captions, and the
About these positions disclosure have been removed. The fan slider is unchanged.
Both airflow diagrams expose five direct-select curves and 48 × 48 px endpoint
buttons. The curves have a wider transparent hit stroke; endpoint buttons support
keyboard activation and announce the axis/position without visible captions.

Vertical positions 1–5 run top to bottom. At the user's request, the proposed
horizontal visual layout puts 1 in the centre, 2–3 left and 4–5 right. Within each
side the design places 2/4 nearer the centre, yielding visual order 3–2–1–4–5.
This is a DESIGN mapping, not newly verified hardware evidence. In particular,
prior observations did not establish relative order 2 vs 3 or absolute position
4 relative to centre. Verify the physical mapping before native implementation
or translating this diagram into live device commands. These caveats belong in
the implementation handoff, not the requested clean customer view.

A click applies immediately in the local simulation. The reported line stays
solid blue while a pending request is dashed with a hollow endpoint. Matching
readback moves selection; failure keeps the previous report. Oscillate remains
independent per axis. Pending blocks additional writes. No native app or appliance
changes were made; user confirmation remains required before app implementation.

Verified: endpoint click; click on the fifth vertical curve itself (not its
endpoint) generated a Position 5 request retaining Position 3 during pending;
keyboard Enter activates horizontal centre Position 1. Existing semantic names,
mode guards, readback separation and reduced-motion rules are retained.

Additional review: unconfirmed horizontal Position 4 preserved selected centre
Position 1. At 320 px with large text and dark appearance there was no horizontal
overflow; all five endpoint targets measured 48 × 48 px.


## New concept: photo + two-dimensional airflow scene (awaiting approval)

Open `airflow-scene.html`; `expanding-controls.html` keeps the prior direct-line
concept for comparison. Both are design artifacts only. The new concept uses
`ac-scene-product.png`, generated with the built-in image_gen tool. Full prompt
and provenance are saved in `ac-image-provenance.json`. This is a representative
product image, not a photo of the user's AC or a camera feed.

- With both axes fixed, drag the stream/handle in two dimensions. The stream
  previews the finger position locally; a 5 × 5 snap grid appears during aiming.
  On release, snap to one supported position on each axis and request only
  changed axes. The horizontal direction bar supports arrow keys and Home/End. Escape or
  pointer cancellation discards the preview.
- Vertical and Horizontal swing are separate switches. Either or both can run.
  Manual aiming is disabled only while both axes swing. Vertical swing permits horizontal aiming; Horizontal swing permits vertical aiming. Switching an axis off
  returns to that axis's last confirmed manual preset. It does NOT send a
  physical freeze-at-current-angle command; cloud code 0 supplies no phase.
- The prototype starts at Vertical 3 / Horizontal 1 with both swings off. These
  are sample reports. Production must use observed device state, not defaults.
  A real missing remembered fixed position would need explicit position choice.
- The blue stream follows reported state except during the explicit local drag
  preview. Pending aim is shown with a dashed outline. The two-axis update is
  simulated sequentially, never presented as an atomic physical change. The
  Horizontal fails scenario demonstrates Vertical succeeding while Horizontal
  keeps its previous report. Refresh does not resend.
- Movement and flow particles are schematic. Vertical/horizontal swing phases
  are synthetic, independent and unsynchronized with hardware. Photo louvers
  are static. No physical angle, current vane phase, airflow measurement or
  camera imagery is inferred. The proposed horizontal order 3–2–1–4–5 remains
  unverified and requires physical mapping checks before production commands.
- Flow particles have a 1.8-second cycle. Illustrative horizontal and vertical
  sweeps have independent periods (~13.8 and ~11 seconds). Reduced-motion OS
  preference or the review toggle disables particles and replaces sweeping
  with a static illustration, while keeping switch state and controls available.
  No haptics or Android screen-reader behavior is simulated.

Browser verification: diagonal drag produced a two-axis request; the corrected
pending outline is visible; a partial failure retained successful Vertical 5
and prior Horizontal 1. Vertical-only and combined swing states were observed,
with the scene path changing over time; reduced-motion path remained identical
and particle animation became none. The 320 px large-text dark preview had no
horizontal overflow. JavaScript syntax and whitespace checks passed.

Native integration, model-specific geometry, real-device command sequencing,
physical freeze semantics, Android accessibility/haptics, and review of an actual
user-supplied AC photo remain outside this prototype. No Android files or live
appliance state were modified.

Follow-up verification: turning both swings off restored the last confirmed
Vertical 5 / Horizontal 1 and re-enabled manual aiming. ArrowRight from the
default centre aim issued a horizontal-only update.

Scene refinement: the circular aim handle is replaced by a horizontal bar with
a 48 px tall invisible touch area. The airflow paths maintain equal spacing
from outlet to bar; the filled band retains a constant horizontal width rather
than converging. The pending bar is dashed. Drag, snapping and swing behavior
are unchanged.

Independent-axis aiming refinement: pointer preview, snapping, pending outline,
keyboard input and simulated writes all exclude the swinging axis. Its animation
continues throughout the gesture and pending readback. The guide grid becomes a
single row or column for the adjustable axis. Only a changed fixed axis is sent;
no aim gesture can silently stop swing. Both swings together disable manual aim.

Independent-axis browser verification: with Vertical swinging, ArrowDown was
ignored and ArrowRight changed only Horizontal 1 to 4. Both swings together hid
the manual handle and displayed the instruction to turn one swing off. With
Horizontal swinging, ArrowRight was ignored and ArrowDown changed only Vertical
3 to 4. A diagonal pointer drag then snapped Vertical to 1 while Horizontal
remained swinging. The saved `airflow-independent-axis-preview.png` shows this
final state. JavaScript syntax and whitespace checks passed.

Swing controls refinement: replaced the two-line switch cards with single-line
Vertical and Horizontal buttons. Removed switch tracks and Fixed/Swinging
captions. Each button has a 36 px visual surface inside a 48 px touch target;
confirmed active swing uses the existing blue/soft-blue semantic colors.
`aria-pressed` exposes selection to assistive technology. Pending behavior and
independent-axis aiming remain unchanged.
Verified both selected states, toggling off, and independent aiming availability
in the browser. At 320 px with large text, both labels fit without overflow and
retain 48 px touch targets. Preview: `airflow-slim-controls-preview.png`.

Added a small Swing heading above the compact Vertical / Horizontal buttons,
with an accessible named group and large-text sizing. Syntax and whitespace
checks passed; heading placement reviewed in the browser.

Quick controls refinement: Display, Powerful and Eco are now sibling compact
buttons below the fan/airflow cards, replacing the static Display on / More row.
They reuse the swing-button treatment, 48 px targets and `aria-pressed`; blue
means confirmed active. Pending sample writes retain the prior reported state.
Display is independent and remains available in the power-off sample. Eco is
available in Cool and confirms the coupled 26° target before enabling; the
sample verifies Eco then the target and does not restore temperature on disable.
Powerful uses the Normal-capacity fixture. Capacity selection and its coupled
confirmation remain outside this focused prototype; production must retain the
contracts in the parent README. No Android files or live commands were changed.
Browser verification: Display toggled off after simulated readback, Powerful
activated, and Eco confirmation completed with the displayed target changing to
26°. At 320 px with large text, all three labels fit and retain 48 px targets.

Quick controls now use three equal grid columns. Browser measurements at 320 px
with large text: each button 84.66 px wide, 48 px tall, no label overflow.
The existing complete More sheet (app-extension.js, moreSheet) contains Eco,
Powerful and Capacity. Capacity remains to be surfaced in this focused scene;
removing the static More placeholder did not implement its capacity picker.

Converti8 filled control: a single 80 px surface combines label, value, fill and
internal discrete stops (Off, 40, 50, 60, 70, 80, 90, 100, 110). It defaults Off,
which maps to Normal automatic capacity, not AC power off. Fill represents the
selected stop, not measured output. No separate slider rail or switch is drawn.
Pointer/keyboard changes preview locally and apply on release; pending retains
reported fill with a dashed requested boundary. Unconfirmed writes retain prior
reported values. Escape/pointer cancellation discards a gesture. The native
range supplies keyboard and accessible value text.

Selecting a fixed capacity with Powerful on confirms Powerful off first;
enabling Powerful with fixed capacity confirms return to Normal first. These
are sequential sample readbacks, not atomic appliance operations. Offline,
power-off, cleaning and unknown-state samples block capacity. Capacity choices
must still be filtered by actual model capabilities in production; this fixture
has no JetStream mode. No live commands or native app changes are made.
Verified in browser: keyboard Off→40%, pointer drag 40→80%, Powerful confirmation
returned capacity to Off, and choosing 110% confirmed Powerful off before setting
capacity. At 320 px with larger text, all internal stop labels fit without
horizontal overflow. JavaScript syntax and whitespace checks passed.
An unconfirmed Off request retained the prior 110% setting with Refresh offered.
Reload confirmed the default Off state. Screenshots document Off and partial fill.

Converti8 Off now collapses to a 48 px row with its label, Off value and chevron.
Tapping opens the existing filled control without changing capacity. Confirmed
Off collapses it again; active capacity remains expanded. Escape closes an
uncommitted Off expansion. Keyboard focus returns to the collapsed launcher.
Browser-verified default collapsed height (48 px), expand/select 110%, and return
to Off with the slider removed from the accessibility tree and focus restored.
