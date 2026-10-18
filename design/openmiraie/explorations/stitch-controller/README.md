> Historical Stitch exploration. The maintained, refined prototype is at `../../prototype/index.html`; native production apps will be built separately from the Android PoC.

# Room AC · Playful Android proposal

Review [overview.html](overview.html) or `overview.png` for the corrected controller and two matching widget proposals. Generated with the approved Stitch design system using synthetic Bedroom/temperature examples. Production Java, APIs, installed app and phone state are unchanged; no commit or push was made.

## Review artifacts

- `controller-corrected.png`: controller viewport shot at390×844 logical pixels,2×rendering.
- `controller-corrected-full.png`: full-height corrected controller,1072logicalpixels; content scrolls and the bottom navigation has an inset.
- `controller-corrected.html`: Stitch HTML with its seven returned DOM edits applied locally. Original remote assets remain in `controller.html/png`; initial exports are preserved too.
- `widget-compact.png`, `widget-wide.png`: readable crops of corrected2×2 and4×2 target-cell proposals. Actual sizes vary by launcher; these specimens render240×240 and324×178logicalpixels. Every visible widget action measured at least48×48logicalpixels.
- `widgets-corrected.png`, `widgets-corrected-full.png`, `widgets-corrected.html`: reviewed widget board. Original exports remain `widgets.html/png`.
- `overview.html`: self-contained image comparison board; its screenshot is `overview.png`.

## Important provenance

Stitch's controller edit returned precise DOM operations and text saying it had corrected the design. Both API readback and live canvas still showed the initial HTML/screenshot. We applied the returned operations to a local copy and removed the original specimen's automatic fake-confirmation handlers. The corrected local proposal has explicit Set/Apply actions, actual On labels, no autosync promise, no unsupported Swing/nanoe branding, and a flat thermostat surface. The live Stitch project should not be presented as containing those persisted corrections.

The widget board needed small local copy corrections: removed a real-time sensor claim, unsupported swing note and invented review version; made DisplayOn visible rather than title-only. The widget display action remains proposed; only the main app currently ships display control. Both widgets refresh manually and show last-known reported state.

## Review and limits

Inspected screenshot and HTML for controller and widgets. Verified four modes only, distinct Power/Display, explicit draft/commit copy, known fan/additional-control entries and manual-refresh semantics. Every Stitch generation/edit text description and suggestion is saved in the sibling `*-insights.json` files; suggestions were not treated as permission to generate further screens.

This is a visual direction proposal, not a native build. The controller needs scrolling at390×844. Native switch geometry, all selected/disabled contrast states, TalkBack, text scaling, window insets, widget resizing and real command/readback behavior need implementation QA. In particular, selected-mode contrast should be mapped to a consistent approved foreground/container pair rather than mixing underscore/hyphen theme aliases. Readings and additional-control sheets are specified in `prompts/behavior-brief.md` but not visually designed in this pass. No continuous logging, humidity, RPM, scheduling, timer or automation support is implied.

Implementation should preserve lightweight native Java Views/RemoteViews and reuse current capability/API/session/SQLite layers. Use system typography, local vector icons and native drawable states; the HTML's external build/font links are design-tool dependencies, not a proposal to ship a web runtime or font download.
