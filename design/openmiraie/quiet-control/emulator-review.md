# Native layout review · 19 October 2026

The app is available in the Android emulator as **openMirAIe · Review**. It uses
production native Views with sample room data, a separate application ID and no
Internet permission. The connected physical phone and live AC were not changed.

## What changed

- Page headers and body now share 24 dp side margins, reduced to 20 dp below
  360 dp. Standard cards and list rows have deliberate internal padding instead
  of icons sitting on their edges; section/card gaps use consistent spacing.
- Fan/Airflow cards align to the top of their row. Previously the interaction
  between vertical centering and an asymmetric bottom margin put their top edges
  outside the parent and clipped the rounded corners. Horizontal containers no
  longer apply automatic text-baseline alignment to compound controls.
- Card labels use an 8 dp icon gap rather than leading space characters. Card
  insets are symmetrical; icons use consistent dimensions.
- The temperature dial's height is capped at 340 dp, with artwork centred in the
  available width. Wide layouts no longer create an enormous square dial.
- Long controller room names use at most two header lines, with the full name
  retained for accessibility and room selection. Large text still scales and
  the content remains scrollable; this is not an attempt to fit everything above
  the fold at 200%.
- The password field grows with text instead of being forced to 54 dp height.
- The Activity keeps its outer inset container across screen updates, explicitly
  owns system-bar insets, and installs its inset listener before attachment.
  This prevents transient loss of top spacing during repeated screen/dialog
  transitions. Individual page scroll positions remain separate.

## Review evidence

Android 15/API 35 emulator, English, gesture navigation. The saved matrix covers
320 dp portrait at 200% font scale, 360 dp portrait at normal text, approximately
412 dp portrait in dark mode, and a wide landscape viewport at normal text.
Actual Android configurations are recorded in each `geometry.txt`.

[Open the screenshot gallery](../../../artifacts/quiet-controller/emulator-review/gallery.html).
[Machine-readable matrix](../../../artifacts/quiet-controller/emulator-review/matrix.json).
[Colour contrast calculations](../../../artifacts/quiet-controller/emulator-review/contrast.json).

The platform instrumentation audits 33 views/scroll positions per configuration:
controller, mode, fan, airflow overview, both airflow sheets, More, Readings,
loaded Energy, Saved Readings, Settings, rooms, device details, long room name,
Dry, Fan, offline, pending temperature and sign-in. It checks real measured
geometry, enabled 48 dp touch targets, text height, children exceeding clipping
parents, and status-bar insets. Scroll containers are deliberately allowed to
have taller content. Bounds overlays were inspected separately from clean
screenshots; representative light/dark/large-text/landscape captures were
visually reviewed. Automated geometry checks do not prove optical perfection.

Debug and review builds, 71 existing JVM tests, lint (zero errors/four existing
warnings) and repository checks pass. The checked active text/background pairs
range from 5.07:1 in light mode to 7.13:1 in dark mode or better; this calculation
excludes disabled controls and does not certify every possible composited pixel.

The audit follows [Android's 48 dp target guidance](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views)
and [Material's spacing guidance](https://m2.material.io/design/layout/spacing-methods.html),
while retaining the approved warm-white/charcoal/blue visual direction.

## Scope limits

This is a native layout and synthetic-state review, not live appliance or full
accessibility acceptance. TalkBack, switch access, physical keyboard, IME/system
file picker, launcher widgets, RTL/localisation, Android 26 and all device/OEM
configurations remain separate checks. Landscape is a scrollable one-column
layout, not a new two-column composition. The dial numeral is fitted to the ring;
it does not scale without limit with Android font size. This pass does not
replace the older reference PNGs or claim every web-preview composition is
implemented. Native spacing fixes documented here take precedence over those
older PNGs.

See [Android testing instructions](../../../apps/openmiraie/android/testing.md#offline-emulator-layout-review)
to reproduce the review build and geometry audit. Changes remain local alongside
pre-existing uncommitted work; no release or contribution was published.
