# Native implementation plan

This is a proposed follow-up, not a production change. The generated design will be reviewed before implementation. Existing capability parsing, authentication, command/readback, SQLite and CSV APIs should be reused.

## Structure

Keep one Activity if useful for the tiny build. Split UI construction into small helpers (`ControlScreen`, `ReadingsScreen`, `ControlSheet`) rather than growing the existing long form. Use a small immutable UI-state model for reported fields, observation time, mode/temperature/fan drafts, and command lifecycle. These are proposed source boundaries, not additional libraries.

`Controls` shows device selection, timestamp/refresh, power, target temperature, modes, fan/display and additional controls. `Readings` shows current known metrics, manual energy loading, snapshot count, raw fields and exports. Overflow carries Choose another AC, Refresh capabilities, Add widget and Sign out. This keeps credentials/debugging out of the everyday path.

## Behavior mapping

| UI | Existing layer | Required behavior |
|---|---|---|
| Power | `Api.powerKey`, command/readback | Use capability `PowerStatus`; explicit desired on/off; do not confuse display |
| AC display | `DisplayControl` | Use actual model key; index8 established for testedmodel; independent of power |
| Mode row | `DeviceProfile.modes()` | Advertised choices only; draft+Apply before dependent controls |
| Temperature | `temperatureRange(mode)` | Actual min/max/step, bounded draft, explicitcommit; hide in Fan-only |
| Fan | `fanSpeeds()` | Actual choices; disable Dry/cleaning; setting notRPM |
| Eco/Powerful/Capacity | Existing sequence methods | Explain setpoint26/mutual exclusions; confirmation before next dependent command |
| Refresh | `Api.status/cacheStatus` | Timestamp last successful observation; record snapshot; preserve lastgooddata on error |
| Readings | `Telemetry.panel` and raw fields | Fractional room°C retained; missing fields `Not reported` |
| Daily energy | Existing on-demand history | kWh, partial-today label, separate CSV |
| CSV | Existing SAF export | No broad storage permission |

## Tokens and native drawing

Use `sp` for text and `dp` for geometry, resolved through one tiny theme/tokens class. Prefer Android system font and built-in weight variants. Card/button radii and colors become shared `GradientDrawable` builders; selected/disabled/pressed backgrounds use `StateListDrawable` and `RippleDrawable`. Icons become small local vector paths after reviewing the actual generated glyphs. System bar colors/insets follow theme and Android's window insets. Use noWebView, CSS/JSruntime, Compose dependency or externalfont pack.

The final graphic design is a visual target. Native text scaling and accessibility can change layout height; do not shrink bodytext or touchareas to match screenshot pixels.

## Widget plan

Retain `AppWidgetProvider` and `RemoteViews`. Supply minimum/resizable layouts via `AppWidgetProviderInfo` and `onAppWidgetOptionsChanged`; choose layout based on available minimum width/height rather than assuming all launchers share cell geometry. New 4×2 display action is an explicitly proposed addition and needs its own `ControlService` path using capability+readback. Each mutating action shares the same serialization and pending/confirmation state as the app. Separate PendingIntent requestcodes/actionstrings avoid collisions. Handle signed-out/noAC/unknown/offline/pending gracefully. Refresh stays manual.

A launcher widget can outlive Activity lifetime. Reuse encrypted selection and cached reportedstate; never embed tokens inRemoteViews/PendingIntent extras. Tapping header/temperature opens controller. Displaytoggle label is drawn from lastconfirmedstate; timestamp remainsvisible. Never pretend a tap succeeded because the foregroundservice started.

## Verification before shipping

1. Build existing native app and run current28checks; add focused tests only where UI-state/mode gating logic changes meaningfully.
2. Test allfourmode layouts with synthetic model fixtures, including emptycapabilities/unknownreadings, offline/failed/unconfirmedstates and longdevicenames.
3. Test fontscale1.0/1.3/2.0, TalkBack, light/dark, rotation and systembarinsets on Android. Touchareas minimum48dp.
4. Test actual reversible commands with authorizedphone and baseline restoration, keeping PiP helper running as requested.
5. Test 2×2/4×2 widgets on launcher resize, refresh, power, proposed displayaction, pending failure, signedoutstate and appprocessrestart.
6. Compare native main and expandedcontent against fullheightreviewshot; measure APK growth and dependency list. Browserpreview does not establishnativecorrectness.
