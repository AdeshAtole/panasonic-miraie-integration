# Capability-driven adapters

## Model interpretation

The shipped model schema includes `controlCapabilities`, `statusCapabilitiesV2`, `configCapabilities`, a legacy `statusCapabilities` object, `category`, `subCategory`, `modelId`, `modelNumber`, `thirdPartyService`, and virtual `devices`. They are not interchangeable sections.

Capabilities carry a `capability` name and often `attributes[]`. The inspected AC code commonly selects the first attribute. Keep the complete attribute list when generalizing; multi-channel switches or other families may need multiple attributes. AC-compatible shapes include:

```json
{
  "controlCapabilities": [
    {"capability":"PowerStatus","attributes":[{"key":"ps","values":["on","off"]}]},
    {"capability":"ACMode","attributes":[{"key":"acmd","values":["auto","cool","dry","fan"]}]},
    {"capability":"Convertible","attributes":[{"key":"cnv","intValues":[110,100,90,80,70,60,50,40,0]}]}
  ]
}
```

This reduced example reflects the tested AC and is not a universal schema. String choices use `values`; numeric choices can use `intValues`. Ranges can be an object or list and may depend on `mode`. AC bounds can be unit/value arrays under `min`, `max`, `increment`; select Celsius rather than whichever unit occurs first. The actual model misspells it `celcius`; support `celsius`, `celcius`, `C`, and `°C` when appropriate. Preserve wire enums and numeric types, even if the UI uses friendlier labels.

Use [capability-registry.json](capability-registry.json) to inspect the 231 statically extracted registry records and recorded collisions.

Registry constructor names can differ from wire capability names (for example, a refrigerator mode registry identifier can map to `RefMode`, not an AC-style name). Multiple registry records can share a capability name or index. Never resolve `ki` by enum ordinal or by a global reverse lookup on integer alone.

A generic adapter should expose unsupported schema shapes as inspectable metadata, not silently coerce them into AC controls. The AC prototype's fallback 16–30°C/step 1 follows original AC behavior; it is not a default for other appliances. Absence of model metadata is not permission to invent controls.

## Reported data

A model can advertise a capability without returning its value, and a status response can contain useful values not listed under status capabilities. The tested AC returned `rmtmp` although its status capability list did not advertise that sensor. Keep a normalized known-values view plus the raw map.

Store raw values with device/model identity internally, UTC observation time, source transport, and any device timestamps. Do not assume an updated cloud timestamp means a new physical sample. Preserve fractional values and numeric strings; use decimal arithmetic for energy totals when precision matters. Unknown units remain unknown.

Treat absent, null, malformed, sentinel, and genuine zero readings separately. `rmtmp:-1` was treated as unavailable in the AC adapter; do not apply that sentinel to every sensor, since negative temperatures can be real. Generic serialized fields such as `ambient_temperature`, `room_temperature`, `pm2.5`, and the literal key `power reading` occur in a `SensorStatus` class; they are not proof any selected device returns them. Language primitive defaults are not observed zeros.

`acfs` is a requested/reported fan setting. It is not a tachometer. Never turn `High` into an RPM estimate or derive watts from an energy total without a verified interval and meaning.

For AC swing, numeric range metadata alone does not identify center, degrees or
oscillation. Use the model-scoped [swing observations](swing.md), including the
observer perspective and corrected horizontal labels. Preserve partial mappings
and corrections with the data exposed to agents; do not infer numeric direction
ordering or turn prior owner observations into new physical confirmation.

## Integrating into other platforms

Map stable device identity to the target platform's device registry. Map capabilities to switches, selectors, numeric controls, climate entities, and sensors only where semantics are established. Keep command methods separate from state updates so widgets, web UIs, CLI tools, and automations share the same adapter. Use server-side credential storage for a web app rather than embedding account tokens in published JavaScript; direct browser CORS support has not been tested.

Choose an explicit collection policy. The tested native app recorded successful refresh/command-readback snapshots in private SQLite, capped at 5,000, with manual CSV exports and no background polling. Those are reference design choices, not MirAIe requirements. If implementing automatic collection, make the interval, retention, restart behavior, and network failure handling explicit. Keep status snapshots separate from Daily/Weekly/Monthly history.

CSV exports should preserve decimals, missing values and timestamps, quote delimiters/newlines, and neutralize spreadsheet formulas in text fields. Redact identifiers, registration tokens, UDP keys/IVs, pairing codes, addresses, topics and credentials before public fixture publication.
