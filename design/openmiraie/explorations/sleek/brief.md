# Room AC design brief

Design proposal, prepared 19 October 2026. No production code or device state is changed by this brief. Screens must use synthetic room names and sample readings, with no account/device identifiers. The intended implementation remains native Android Java Views and RemoteViews, min API 26; no web runtime, React Native, downloaded fonts or runtime asset libraries.

## Product purpose

A fast, calm AC remote that puts power, temperature, mode and the AC's illuminated display within easy reach. Open the app, understand the last reported state, change one thing deliberately, and see whether the AC confirms it. Widgets should make the most frequent actions possible from the launcher.

## Visual direction

Quiet, precise and tactile. Use a deep ink neutral foundation in dark mode, warm near-white surfaces in light mode, and a restrained cool jade accent for selected and confirmed states. Use Android system sans-serif typography with large tabular temperature numerals, compact medium-weight section labels and readable secondary copy. Favor a familiar single-column layout, one large temperature area, four evenly sized mode choices and clearly separated controls rather than a decorative thermostat dial. Rounded rectangular surfaces, hairline tonal borders and minimal elevation keep the UI native and efficient. Use small, simple line icons embedded as local vectors, no photos, illustrations, glass blur or animated background. Distinctive details: the target numeral has a clearly subordinate room-temperature reading; a persistent AC display tile makes night-time display control as accessible as fan speed. A small read-time line and refresh action express that this is cloud-reported state, without making the screen feel like a diagnostics console.

## Design output

One compact design run: main controller in dark theme, main controller in light theme, and a widget board showing two Android home-screen widget sizes (2×2 and 4×2 cells). The actual widget dp dimensions vary by launcher; minimum touch sizes and text scaling take priority over exact cell measurements. Include a short expanded controls/data sheet if budget permits; all required secondary content can be documented in the spec rather than spending a separate design run.

## Main controller

- Header: Room AC identity, synthetic `Bedroom` device/room selector, overflow for account and device management.
- Status: `On · Cool`, `Read 10:24`, an accessible Refresh button. Read time describes the observation, not a fresh sensor measurement.
- Power control: clear, explicit turn-off action when reported on; turn-on when reported off. Unknown power must not be rendered as off.
- Target: prominent `23°C` sample, separate `Room 24.18°C` sample, minus/plus draft controls, explicit `Set 23°C` commit when changed. No command on every slider movement; avoid a dial that implies otherwise.
- Operating mode: Auto, Cool, Dry, Fan only, derived from capabilities. Changing selection offers `Apply Dry` before dependent controls can be edited. Do not render Heat for this known model.
- Fan: reported Auto setting with selector sheet for Auto / Quiet / Low / Medium / High. Selection remains a draft until Set.
- AC display: independent On/Off tile, immediately discoverable on the main screen. Explicit action `Turn display off` or `Turn display on`; distinguish its illumination icon from AC power.
- Additional controls: `Eco · Powerful · Capacity` entry opens a sheet. Keep these discoverable but out of the primary temperature area.
- Readings & history: room temperature, fan setting, connectivity, on-demand daily energy, snapshots and exports live in a secondary screen/sheet. Do not suggest continuous logging or chart fabricated history.
- Bottom navigation, if used, must have only meaningful destinations: Controls and Readings. No scheduling or automation tab.

## Compact widget designs

Widget actions are a proposal beyond the current shipped widget, whose only existing controls are On, Off and Refresh. Avoid suggesting additional proposed buttons already work.

2×2: synthetic room name opens controller; target temperature and mode; small last-read time; explicit power action and refresh. Target is read-only at this size so actions fit 48dp touch targets. Power icon must have a text/accessibility label.

4×2: synthetic room name, target and room reading, mode and read time; explicit power, proposed AC display toggle and Refresh actions. Temperature +/- may be omitted to preserve reliable touch targets and deliberate commit semantics. Tapping the temperature opens the app. Last-known state remains visible while refreshing; provide pending/error status in place of the read-time line. No automatic refresh promise.

## Rules that must survive the redesign

- Power on is required for mode, temperature and fan changes.
- Fan-only hides target-temperature controls, showing an explanatory air-circulation label.
- Dry disables manual fan setting and says the AC manages it. Do not promise a forced Low reading.
- Active evaporator cleaning disables temperature/fan. This design does not introduce a cleaning command.
- Mode preview must be applied before controls for that mode become active.
- Eco is Cool-only, with an upfront `Also sets 26°C` explanation.
- Powerful is Cool-only and unavailable during cleaning; it disables fixed capacity. A fixed capacity disables Powerful. Show that consequence before committing.
- Advertised capacity options: Normal (wire0), 40,50,60,70,80,90,100,110%. Do not invent55% or an energy-saving percentage.
- AC display is independent of AC power. Do not gray it out solely because AC power is off; availability follows actual reported control support/state.
- Pending, requested and reported states are distinct. A command accepted by the API is not proof of physical success.
- Unknown/missing telemetry is `Not reported`, never zero.
- Fan setting is not measured RPM. No measured RPM, humidity or compressor speed is established.
- Daily energy is kWh and today may be partial; load on request. Snapshots are saved on refresh/command readback/widget refresh, max5000, with no periodic background logger.
- CSV reading export and energy export stay separate; Android document picker requires no broad storage permission.

## State contract

Idle confirmed: show reported values and observation time. Draft: visually label selected-but-unapplied value; show commit/cancel; dependent mode controls disabled. Pending: show `Setting 23°C…`; show last confirmed22°C alongside requested23°C, disable overlapping commands, no success checkmark yet. Confirmed: replace reported value after readback and announce `Temperature set to23°C`. Accepted/unconfirmed: retain reported state and show `Not confirmed yet · Refresh`; do not blindly repeat the mutation. Failed: retain reported state, error and deliberate retry. Offline: mark last-known values and timestamp, disable mutation controls, allow Refresh. Stale: timestamp gets an explicit older-state label (suggested UI threshold5minutes, a UI freshness heuristic rather than a sensor truth claim). No status yet: placeholder target `—`, no invented defaults, Refresh prompt. Signed out: focused email/mobile and password flow, autofill, password visibility control, concise direct-to-Panasonic/privacy copy.

## Accessibility and native feasibility

Minimum48dp touch targets, dynamic text without clipped units or controls, 4.5:1 body contrast, labels beyond color, TalkBack names for target/room/display/power, logical focus order and announcements on confirmation/error. Avoid requiring precision drag. Respect system dark/light setting and system bar insets. Use rounded GradientDrawable/StateListDrawable backgrounds, RippleDrawable press feedback, system font weights, native Buttons/TextViews and Dialog sheets; RemoteViews-supported widget layouts. Keep the account/session/API/SQLite layers unchanged. No additional runtime dependencies are required by this visual direction.
