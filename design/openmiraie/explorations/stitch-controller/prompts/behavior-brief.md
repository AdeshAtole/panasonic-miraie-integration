# Room AC behavior brief for design review

This is a design proposal for the existing lightweight native Android Java Views/RemoteViews app. It does not authorize production app, phone or account changes.

The controller offers actual advertised modes Auto/Cool/Dry/Fan; no Heat on the known model. Temperature16–30°C step1. Fan Auto/Quiet/Low/Medium/High. Power on gates mode/temperature/fan. Fan-only hides temperature; Dry disables manual fan without forcing Low. Cleaning disables temperature/fan. Draft mode is explicitly applied before dependent controls. Draft target/fan is explicitly committed. Actual reported state updates only after bounded readback; offline/failed/unconfirmed state must not look like success.

AC displayOn/Off is an independent quick control, already shipped in the main app. Power usesPowerStatus, display usesDisplayControl; never map the two to the same command. Widget display control is still proposed.

Eco is Cool-only and sets26°C when enabling. Powerful is Cool-only and unavailable during cleaning. Powerful disables fixed capacity and fixed capacity disables Powerful. Normal0 and40/50/60/70/80/90/100/110% capacities are advertised. Put consequences in sheets/confirmation copy, not all on the primary controller.

Room temperature is available with fractional precision; fan is a setting, not measuredRPM. RSSI/online/errors/warnings/raw fields and on-demand dailykWh are available. No measuredRPM, humidity or compressor frequency is established. Today can be partial. Snapshot recording occurs on manual refresh/command/widget readback, capped5000, no periodic background logger. Reading CSV and energy CSV exports are separate. No continuous-history graph is implied by snapshot storage.

Use synthetic Bedroom/23°C/24.18°C/10:24 sample values. Keep account/device identifiers and credentials out of design inputs/outputs. Main UI copy should be natural: Updated10:24, Auto, DisplayOn. No protocol/debug copy, raw fields or unsupported actions in the everyday control hierarchy.

The generated visual direction comes exclusively from the approved project design system. Controller/widget generation prompts intentionally contain layout/content/behavior only, without palette/font/shape tokens. Rendered native implementation, accessibility, widget resizing and real actuation would need separate verification after design approval.
