# AC swing: verified scope, commands and observed positions

Updated **2026-10-19 (Asia/Kolkata)**. Protocol source: MirAIe India Android
**01.4.32 / 231301432**, base APK SHA256
`62a36f534b6e38802e9a1ff2138a6342d1328e0f0a4928ce37ba2299e2f867a1`.
Static builders were inspected with REA 6.7.0 and JADX headless MCP 0.7.1
(engine SHA256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`).

## Evidence and scope

- **Static only:** the shipped range builders, setters, capability indexes and UI
  guards described below. Static code alone does not prove appliance movement.
- **Live cloud readback:** one MirAIe India AC, model **140013 / CS-CU-NU18BKY5WX**,
  using cloud REST through the MCP server. Both axes' codes 0–5 have been reported
  or command/readback tested; individual physical observations are listed below.
- **Owner observed:** the movements below, with power on, Cool mode and cleaning
  off. Firmware was not captured; applicability to another firmware/unit, calibrated
  angles and physical end stops are unverified.

Use the current tables below. The former horizontal **3=center label is withdrawn**
after the direct 1→3 comparison. Do not infer a complete left-to-right ordering
from numeric codes or transfer the vertical mapping to horizontal swing.

## Current vertical observations

| Code | Cloud evidence | Owner observation |
|---|---|---|
| 0 | Accepted and matched | Continuous vertical oscillation |
| 1 | Reported baseline; later explicitly commanded and matched | Starting position; absolute angle/fixedness not separately characterized |
| 2 | Accepted and matched | Moved downward from 1 |
| 3 | Accepted and matched | Moved farther downward from 2 |
| 4 | Accepted and matched | Moved farther downward from 3 |
| 5 | Accepted and matched | Moved farther downward from 4 and stayed fixed; lowest tested code |

Vertical 0 is observed oscillation. Codes 1→2→3→4→5 form the observed downward
sequence, with 5 the lowest tested fixed position. These are position codes,
not degrees or a calibrated physical maximum.

## Current horizontal observations

Left/right means **the observer's left/right while facing the AC**.

| Code | Cloud evidence | Owner observation |
|---|---|---|
| 0 | Accepted and matched | Continuous horizontal oscillation |
| 1 | Accepted and matched, including retest from 2 | Appeared centered / straight ahead; exact angle/fixedness not separately established |
| 2 | Accepted and matched | Farther left than 3; leftmost observed preset, per later owner clarification |
| 3 | Accepted and matched, including direct comparison from 1 | Moved slightly left from 1; earlier center label withdrawn |
| 4 | Accepted and matched | Moved observer-right from 3 |
| 5 | Accepted and matched; later status still 5 | Moved farther observer-right from 4 and stayed fixed; rightmost tested code |

**Correction provenance:** 3 initially appeared centered. After 1 also appeared
centered, an authorized 2→1 retest again looked centered, and the direct 1→3
comparison moved slightly left. The current mapping therefore uses 1 as
owner-observed apparent center and 3 as slightly left of 1. Code 2 was separately
observed left-facing. Later on 19 October 2026, the owner explicitly confirmed
that the two left positions should be swapped in the controller: the left-to-right
order is **2, 3, 1, 4, 5**. Code 2 is the far-left observed preset and code 3 is
inner-left. This clarification is owner-reported evidence, not a new automated
actuation/readback session. Neither endpoint should be called a universal physical
end stop. Prior observations never automatically confirm a new command.

## Model binding and payloads

| Capability | Key on inspected model | Native index (`ki`) | Wire type |
|---|---|---:|---|
| VerticalSwing | `acvs` | 5 | Integer position |
| HorizontalSwing | `achs` | 6 | Integer position |

Resolve keys and allowed values from the actual model. Indexes come from
`BaseCapability.Type` in [the registry](capability-registry.json), not array order.
The inspected model advertises integer bounds 0–5 for both axes. Do not transfer
these mappings to another model, IR or partner transport without evidence.

Use the [platform envelope](platform-api.md), for example horizontal oscillation
when code 0 is advertised and the owner requests it:

```json
{"deviceId":"<selected-device-id>","control_params":{"cnt":"an","sid":"0","achs":0,"ki":6}}
```

`ACDevice.setVerticalSwingPosition(int)` and `setHorizontalSwingPosition(int)`
select the corresponding range capability and send its `setNewValue(int)` result.
`RangeCapability.buildMessage(int)` and its JSON-object overload add the model key
as an integer and the capability index. No extra swing companion fields were
observed in those setters/builders.

## Validation, readback and agent guidance

- Require advertised capability/range/list, known online state, power on and
  evaporator cleaning off. The shipped UI also gates pending requests.
- The shipped UI cycles integer choices when supplied; otherwise it increments
  by one through the advertised maximum. The known-model MCP adapter restricts
  codes to 0–5 and respects narrower advertised bounds/lists/steps. Missing or
  ambiguous metadata fails closed. Do not invent degree, direction or off values.
- Send once, wait at least three seconds before the MCP first readback, and use
  bounded follow-up reads. An accepted command or matching cloud report is not
  physical proof. If unconfirmed, inspect status rather than blindly resend.
- MCP agents receive model-scoped observations through `capabilities.swing`, the
  `capabilities` / `control_ac` descriptions and `swing_guidance` in command results.
  Only advertised positions are included; the observer perspective, date, scope,
  partial mapping and remaining unknowns travel with the data. API values remain
  integers; semantic interpretation does not bypass capability validation.

At the end of the authorized session, the owner explicitly requested **horizontal 1
and vertical 1**; both commands matched cloud readback. This was a chosen setting,
not an automatic test restoration. Future testing or changes need their own intent.

## Retained evidence identifiers

Complete records were saved privately during analysis; only compact facts are
included here. These IDs are provenance, not publicly resolvable links.

| Method / inventory | Evidence ID |
|---|---|
| Swing UI class inventory | `ev_59c15c7ec61769095c065301983853910e5e35a9c170c1519eff6b34066aa713` |
| Range capability inventory | `ev_4ffd36eea0ac6fa15c61a195d6607e62d401775a1f70d8059feb1fb6492e4434` |
| `SwingControlViewHolder.onViewClick()` | `ev_80a7c893bf2f75128a8797eae60fa02ef6f070c42f3881be753f86a80fad516e` |
| `SwingControlViewHolder.isCapabilityActionAllowed(boolean)` | `ev_31e0849bac728419de549ba3af4262ce14a9b302ce8d28e03f79a5124d6ebc22` |
| `SwingControlViewHolder.checkAndUpdateSwingValues()` | `ev_922cb93f3c433259cd1174310ceac884fe35946ce1b9c0223759f0a1fd145bbc` |
| `SwingControlViewHolder.refreshUI()` | `ev_fff924bff8fdd8141591ad24f77f91e3094ea74b3950cd79c2e73a6990954595` |
| `ACDevice.setVerticalSwingPosition(int)` | `ev_92c55ba3ba26940ba43fdadf022eb65997314ab99adffb23bbfe852ec52b3137` |
| `ACDevice.setHorizontalSwingPosition(int)` | `ev_fb04daa51f99424a8747aa8ac581967efbcbc8d5219011c365c85e4b53882184` |
| `RangeCapability.buildMessage(int, JsonObject)` | `ev_a0ac5f114c4080ba3fbe1dc7ae00cc26955580db075eda090c72f932a8e48987` |
| `RangeCapability.setNewValue(int)` | `ev_23ef1e551d4a2086e1b92d58573c779c600d0bf97bb34b38b6d476dfca6af9e4` |
| `RangeCapability.buildMessage(int)` | `ev_5e4948706f5ca873657b57a0d3e0325ca96cdb4c53c8ce3fb48e59afbbbb5bc2` |
| `RangeCapability.getIntValues()` | `ev_7cd7ac4dc349ac6118edae483059e8c36fc8a7ecb53df229fa31b5e91b951146` |

The [registry provenance](capability-registry.json) independently records the
index bindings. Static searches covered these exact classes/methods; they do not
establish all swing UIs, modes, other firmware or ecosystem-wide behavior.
