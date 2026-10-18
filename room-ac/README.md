# Room AC 0.3

Lightweight native Android alternative for the connected Panasonic MirAIe AC. Package: `in.room.ac`. No third-party runtime libraries are bundled.

## Controls

The app loads the actual model capability definition, caches it for one day, and offers only advertised choices. This device (`CS-CU-NU18BKY5WX`, model 140013) advertises Auto, Cool, Dry, Fan, five fan settings (Auto, Quiet, Low, Medium, High), 16–30°C in 1°C steps, Eco, Powerful, and Convertible values 110/100/90/80/70/60/50/40/0.

Fan-only hides the temperature control. Dry disables manual fan speed and leaves its fan behavior to the AC. A separate forced fan command interfered with the live Dry transition, so it is not sent. A selected mode must be applied before its temperature/fan controls become active. Controls reflect the reported state after bounded command/readback checks.

Additional modes are in a small dialog. Eco is available in Cool and sets 26°C, matching the native app. Powerful is available in Cool while cleaning is off; enabling it disables Convertible. A fixed Convertible setting disables Powerful. Native capability indexes are used: mode 0, power 1, temperature 2, powerful 3, fan 4, eco 10, convertible 28, and per-mode temperature 257 where advertised. The model metadata spells Celsius `celcius`; the parser supports that spelling and normal Celsius names.

The main screen includes an AC display toggle when the model advertises both on/off values. It shows the last reported display state and waits for command readback. Display control uses `acdc` with native index 8 on the tested model. Live off/on readback passed, with power, mode and temperature unchanged; the original display state was restored.

Power uses the actual `PowerStatus` capability (`ps` on this model), separate from display control (`acdc`). This corrects the initial prototype's binding in both app and widget.

## Readings and recording

Authenticated status reads on this device expose fractional room temperature (`rmtmp`), target temperature, mode, fan setting, swing positions, multiple feature flags, firmware-reported fields, RSSI, online state, timestamps, errors and warnings. The readings panel shows known values; **All reported fields** preserves the remaining wire fields for inspection. A fan setting is not measured RPM. No measured fan/compressor RPM or humidity reading has been established in this unit's mobile-status response.

Every successful user-initiated status read (including widget refresh and command readback) is stored as a private SQLite snapshot. The total store is capped at the latest 5,000 samples. There is no periodic background polling or persistent monitoring service. UTC observation time and original reported timestamp fields are preserved; observation time does not imply a new device measurement. Missing and unsupported fields are not filled with zeros; raw sentinel values remain raw fields, and the room-temperature panel treats -1/null/nonfinite values as unavailable.

**Export readings (CSV)** uses Android's file picker and needs no storage permission. It exports the selected device's snapshots with UTC observation time, source, and raw reported fields, preserving decimal values.

**Load recent energy use** queries the original app's Daily energy endpoint for today and the previous six days. Live history returned seven entries for this device. The original app displays these values as kWh, rather than instantaneous watts. The current day can be partial. **Export energy history (CSV)** exports separate ISO dates and kWh columns. Energy is loaded only when requested; it is not polled in the background.

## Privacy and build

Sign-in goes directly to Panasonic over HTTPS. Passwords are never saved. Access/refresh tokens and model selection are encrypted with Android Keystore AES-GCM in private preferences; backup is disabled. No credentials or account tokens are copied to project artifacts. Diagnostic export strips account identifiers and credentials and includes only the model/status data needed for this investigation.

Build: `python3 build.py`; output `room-ac.apk` using the local development signing key. Tests: `python3 tests/run.py` (JVM tests use the existing local Android JSON library only as a test dependency). This build remains a small native APK; no cloud server or Home Assistant installation is required.

## Verification and limits

Protocol facts and provenance are included in the public [MirAIe skill](../skills/panasonic-miraie/SKILL.md), especially its AC, platform API, and evidence references. Private runtime captures and bulk decompiler output are excluded from the repository. Live checks confirmed the four advertised operating modes, Fan-only temperature hiding, Dry fan locking, Eco at 26°C, Powerful, capacity at 80%, and restoration to normal settings. Both Android CSV save flows and existing widget refresh/power-on/readback passed. All 28 JVM checks pass. See the root README for the build/test prerequisites and known platform assumptions.

This version does not implement continuous MQTT streaming, automatic background recording, active sleep-profile cancellation, timer scheduling, or full visual redesign. Other unobserved firmware/server metrics remain unknown rather than inferred. The home-screen widget refreshes manually and controls the currently selected AC.
