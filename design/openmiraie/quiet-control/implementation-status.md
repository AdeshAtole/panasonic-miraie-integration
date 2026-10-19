# Quiet Control — Android implementation status

Updated 19 October 2026. The user approved the connected design, asked to preserve it for implementation agents, and authorized native Android implementation. This is the first native implementation pass, not final device acceptance.

## Start here

- [Approved visual and behavior handoff](README.md)
- [Connected full-app preview](quiet-app.html)
- [Controller reference](reference/controller.png)
- [Full-app overview](reference/app-overview.png)
- [Reference hashes](reference-manifest.json)
- [Native Android project and build instructions](../../../apps/openmiraie/android/README.md)
- [Durable agent instructions](../../../docs/agent-instructions.md)

The root AGENTS.md is a read-only, regenerated project-mirror file. The durable guide and design index point here; the root entry could not be updated. The approved design and prior native baseline were committed and pushed in checkpoint `70faeaa`. The inline refinement is documented separately below; do not stage unrelated working files.

## Implemented in the first pass

- Warm-white/charcoal/blue light and dark resources, shared with native sign-in, rooms, settings, control sheets, readings and launcher widgets.
- Room header, mode sheet, charcoal power action, native temperature dial, Fan/Airflow cards, a directly tappable display status and More.
- Control / Readings / Settings navigation. Energy and Saved Readings have separate destinations. Native document exports and launcher placement remain intact.
- Stock Lucide 0.468.0 icons, with original geometry converted to Android vectors. License and source hashes are included under Android `third-party/lucide`; the license is packaged and available in About.
- Native 270-degree relative dial, no touch-down jump, capability-derived bounds/increments, end stops and no inertia. One temperature submission on release; no temperature Set button or ordinary confirmation.
- Keyboard arrows and Android adjustable-range accessibility actions coalesce for 450ms. Cancellation, extra pointers, focus loss, navigation, backgrounding and detach discard unsent edits. Unlike the web preview's blur-commit, native focus loss deliberately cancels to avoid an interrupted edit issuing a command.
- Application-scoped pending target and prior reported target remain separate from observed state. Existing send-once, bounded readback, coupling, model-specific airflow and offline/cleaning restrictions remain in the protocol layer.
- Mode applies immediately on tap; the later inline refinement replaces the main fan/airflow/capacity Set interactions (exact-position fallback sheets remain); Eco/Powerful/capacity confirmations are preserved. Sheets enter over 240ms only when Android animators are enabled; haptics use system-respecting detents.

The native app retains Java Views/RemoteViews and has no web runtime. It follows Material interaction conventions but does not depend on the Material 3 component library. The rotary dial is custom; the icons are stock.

## Verification and limits

Debug build passed. All 71 JVM tests passed (37 protocol, 22 airflow, 12 temperature interaction). Lint completed with zero errors and four warnings: existing toolchain/target versions and localization. Repository checks and diff whitespace checks passed. Detailed results and the APK hash are recorded in [verification.json](verification.json). Twelve new temperature interaction tests exercise no-jump, boundary crossing, clamping, fractional increments, cancellation, no-change/no-write and one-time consumption. Existing protocol and model-scoped airflow tests remain required. These are model tests, not Android touch/accessibility instrumentation.

The redesigned app has now been installed and reviewed on the connected Android 16 phone with user authorization. See [device review](device-review.md) for the sampled checks, fixes and remaining gaps. No appliance command was issued. The reference screenshots remain approved design screenshots, not screenshots of the native build.

## Remaining acceptance and refinement

1. Scoped Android 15 emulator layout review completed at 320/360/412 dp, 200% text, light/dark and landscape: see [layout report](emulator-review.md). Broader device/OEM configurations and display-unknown recovery combinations remain. The dial numeral fits the ring; it does not scale without limit.
2. TalkBack, switch access, physical keyboard, gesture cancellation, interruption, rotation and process recreation. The Activity still rebuilds its hierarchy when repository updates arrive; check focus stability during every pending/readback transition and refine before accessibility acceptance.
3. Test native system Back/insets, IME, modal focus return and system motion settings. Evaluate the 240ms sheet entry with a motion recording; press feedback currently uses native ripple rather than the preview's bespoke scale timing.
4. Finish visual fidelity of the supporting screens: energy day-detail composition/chart, richer saved-reading detail, widget configuration specimens and remaining account/recovery compositions. Current real data/export/recovery behavior is preserved, but not every preview composition has a one-to-one native screen yet.
5. Validate refreshed widgets at compact/wide/resized bounds and after sign-out/restart. Native file-picker failure/cancellation and secure session behavior require device checks.
6. Editable Figma screen assembly remains blocked by the previously reported Starter MCP quota. The new Figma file has foundations and a blank main frame only; the local preview/handoff is authoritative.

Installation, live actuation, publication and messaging another implementation agent remain separate actions. Do not resume the superseded lilac visual design, add a temperature Set button, queue commands, or replace model-specific restrictions with preview fixtures.

## Approved inline refinement implemented — 19 October 2026

Expanding fan/airflow, draggable illustrated airflow with independent swinging axes, compact equal-width utility buttons, and filled/collapsing Converti8 now run in the native app. [The implementation report](native-inline-review.md) records 85 passing JVM tests, the emulator interaction/layout checks, adaptive behavior, pending state and remaining device/accessibility limitations. This supersedes the first-pass sheet interaction description above.
