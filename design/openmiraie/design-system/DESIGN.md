---
name: openMirAIe Playful Android
colors:
  surface: '#FFFBFF'
  on-surface: '#241B35'
  surface-container-low: '#F7F0FF'
  surface-container: '#F0E7FA'
  outline-variant: '#D7CBE2'
  primary: '#6750A4'
  on-primary: '#FFFFFF'
  primary-container: '#EADDFF'
  on-primary-container: '#21005D'
  secondary: '#715B00'
  on-secondary: '#FFFFFF'
  secondary-container: '#FFE8A3'
  on-secondary-container: '#342A00'
  tertiary: '#A43E35'
  on-tertiary: '#FFFFFF'
  tertiary-container: '#FFDAD4'
  on-tertiary-container: '#410002'
  error: '#BA1A1A'
  on-error: '#FFFFFF'
typography:
  display-lg:
    fontFamily: Roboto
    fontSize: 88px
    fontWeight: '700'
    lineHeight: 96px
  headline-md:
    fontFamily: Roboto
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  body-base:
    fontFamily: Roboto
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label:
    fontFamily: Roboto
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
rounded:
  sm: 12px
  md: 24px
  lg: 32px
  full: 9999px
spacing:
  unit: 4px
  sm: 8px
  md: 16px
  lg: 24px
  gutter: 20px
---

# openMirAIe — Playful Android

## Intent

Design a fast, friendly native Android AC remote with a playful Material You / Material 3 Expressive visual language. Replace the rejected sober ink-and-jade dashboard. The implementation target remains lightweight Java Views and RemoteViews; this document does not add runtime dependencies or change the installed app.

## Color system

Light first: warm near-white surface #FFFBFF, dark violet ink #241B35. Primary violet #6750A4 with white foreground; primary tonal container #EADDFF with #21005D foreground. Warm yellow accent container #FFE8A3 with #342A00 foreground. Coral secondary container #FFDAD4 with #410002 foreground. Use tonal colors to group controls rather than a rainbow of unrelated states. Error uses #BA1A1A with white foreground; do not use coral decoration as the only error cue.

Dark counterpart: deep violet surface #1C1724 with #E8E0EF text, lilac primary #D0BCFF with #381E72 foreground, warm yellow #E8C66A with #382C00 foreground, muted coral #FFB4A8 with #690005 foreground. Preserve sufficient contrast; disabled states remain distinct from enabled actions.

## Typography

Android system sans-serif / Roboto, no downloaded font requirement. Hero temperature 72–88sp equivalent, bold expressive numerals with subordinate unit and clear Target label. Header 28–32sp; section labels 16–18sp; readable body 14–16sp. Support dynamic text rather than rigid screenshot sizing.

## Shape, layout and motion

Native Android top app bar and navigation patterns, large touch-friendly segmented mode controls, pill actions, and labeled switches. Tonal surfaces with 24–32dp corners; fully rounded buttons. One expressive asymmetric temperature/control composition rather than a repeated stack of bordered cards. A small vector cooling motif can add personality. Minimal elevation; no glass, photos, heavy gradients or web dashboard chrome. Optional brief shape/press feedback, respecting reduced motion. Minimum 48dp interactive targets and visible labels independent of color.

## Controller contents

Synthetic room name; current power/mode; observation time and manual refresh. Prominent target with minus/plus draft controls and explicit Set action, subordinate fractional room temperature. Nearby independent AC power and AC display controls. Modes Auto, Cool, Dry, Fan only; fan selector; secondary Eco/Powerful/Capacity sheet; Readings destination for on-demand energy, snapshots and exports. Keep everyday copy short.

## Behavior boundaries

Show actual reported state separately from requested state. Mode changes need an Apply affordance before dependent controls. Fan-only hides temperature, Dry disables manual fan, cleaning disables fan/temperature. Power on is required for mode/temp/fan. Eco is Cool-only and sets26°C; Powerful and fixed capacity exclude each other. AC display is independent of AC power. No Heat, fabricated humidity/RPM, schedules or live charts. Daily energy is on-demand kWh; recording is refresh/readback snapshots, not background monitoring. Include pending/unconfirmed/offline/unknown behavior in the handoff.

## Widgets

Two proposed native Android launcher sizes: compact2×2 with target/mode/read-time, power and Refresh; wide4×2 adds room temperature and proposed display toggle. Widget target is read-only. Show a single coherent state in each specimen, with clear current power and desired action. No automatic-refresh promise. Match controller personality while fitting RemoteViews and launcher resizing.

## Planned generation

Controller light, matching controller dark, and widget comparison board. Synthetic data only. Design proposal and exported assets only; no changes to production app, phone state, commits or publication.
