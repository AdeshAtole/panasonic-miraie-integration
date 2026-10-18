# Air conditioners: verified example and limits

Live-tested model: **CS-CU-NU18BKY5WX**, modelId **140013**. These are public model identifiers, not account/device IDs. Other models require their own metadata.

## Mapping on this model

| Capability | Wire key | Native `ki` | Model values / type |
|---|---|---:|---|
| ACMode | `acmd` | 0 | `auto`, `cool`, `dry`, `fan` |
| PowerStatus | `ps` | 1 | `on`, `off` |
| ACTemperature | `actmp` | 2 | Celsius numeric string; 16–30, step 1 |
| PowerfulMode | `acpm` | 3 | `on`, `off` |
| ACFanSpeed | `acfs` | 4 | `auto`, `quiet`, `low`, `medium`, `high` |
| DisplayControl | `acdc` | 8 | `on`, `off`; independent of AC power |
| ECOMode | `acem` | 10 | `on`, `off` |
| Convertible | `cnv` | 28 | Integers 110/100/90/80/70/60/50/40/0 |
| ACModeTemperature | model-dependent | 257 | Static registry alternative; not advertised on this model |

The index comes from the native registry/deserializer, not the model attribute position. Resolve keys from the actual model even when using these indexes. Heat exists in the app's generic code but was not advertised on this unit. The registry includes capacity 55 for other models; this unit does not advertise it. Zero means normal/off capacity restriction, not power off.

**Power is `ps`, not `acdc`.** This model's `acdc` capability is display control. Display off/on was live-tested through Room AC 0.3 with cloud readback confirming both states while power, mode, and target remained unchanged; the original display state was restored. Physical LED observation was not separately captured. The initial community-derived prototype got this wrong; the live model and native PowerStatus setter corrected it. Do not copy a generic short-key table without resolving capability semantics.

For example, request Low fan with `{"acfs":"low","ki":4}` or 21°C with `{"actmp":"21.0","ki":2}`, inside the platform control envelope. Verify actual readback. A command builder is not sufficient evidence of physical support in every mode.

## UI and command dependencies

- Fan-only hides temperature controls. Dry disables manual fan selection, as does evaporator cleaning.
- The original UI attempts Low when entering Dry. Sending an extra fan command immediately after mode change interfered with the live transition in the prototype. The final tested implementation sends the Dry mode command alone, disables fan controls, and displays actual readback; do not promise the returned fan setting will say Low.
- Apply a selected mode before enabling its temperature/fan controls. Previewing a mode must not accidentally send a setting to the old mode.
- Eco is Cool-only on the tested model. Native enabling sets 26°C. The working integration sends Eco on, waits for confirmation, then sets 26°C; off only disables Eco.
- Powerful is available with power on and cleaning off, excluding Auto/Dry/Fan. That means Cool-only for this model, not necessarily every model. Enable by disabling fixed capacity first, then Powerful, with readback between commands.
- A nonzero capacity setting disables active JetStream, otherwise active Powerful, in the original setter. The tested model exercised Powerful only. Respect model-specific alternative features rather than assuming no JetStream exists.
- Enabling native power can raise a below 24°C target to 24°C except in Fan-only and can reset an active sleep profile. The custom app deliberately sends power alone to preserve the user's target. Separate native frontend policy from required wire protocol.
- Native Eco/Powerful/power operations can reset an active non-default sleep profile. Reset uses `acsp:"off"`, the profile capability index, existing `pid`, and `temps`. `acsp:"none"` means an inactive profile identifier/status, not the reset command. Active-profile cancellation was not implemented or tested in the custom app; do not invent a single-field reset.

## Telemetry observed live

`rmtmp` returned fractional room temperature (e.g. 22.61°C). Other returned fields included target/mode/fan, `ps`, horizontal/vertical swing (`achs`,`acvs`), Eco/Powerful/capacity and other feature flags, firmware field `V`, `rssi`, `onlineStatus`, `ts`, connection/update timestamps, `errors`, and `warnings`. Preserve raw fields whose semantics are unresolved.

`acfs` is a setting, not measured RPM. Measured fan RPM, compressor RPM/frequency and humidity were not established in this unit's mobile status or the bounded static AC search. Do not claim the whole platform can never supply them. A humidifier capability is not evidence of a humidity sensor. Filter-health capabilities may exist without a live reported value.

Daily energy history was returned for seven days and interpreted by the original app as kWh. Do not confuse it with instantaneous power. See platform-api.md for query formats.

## Verification achieved

Authenticated sign-in, discovery, model lookup, status reads, temperature changes, all four advertised mode readbacks, Fan-only temperature hiding, Dry fan disabling, Eco 26°C/on/off, Powerful on, capacity 80% disabling Powerful, normal capacity restoration, widget refresh/power-on/readback, and status/energy CSV exports were exercised. 28 local capability/telemetry/CSV checks passed. This is one unit and firmware/account combination; token expiry refresh, active sleep cancellation, all possible enum values and all failure/offline modes remain unverified.
