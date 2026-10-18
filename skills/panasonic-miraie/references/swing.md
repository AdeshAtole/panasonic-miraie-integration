# AC swing: command evidence and observed positions

Scope: MirAIe India Android **01.4.32**, version code **231301432**, base APK
SHA256 `62a36f534b6e38802e9a1ff2138a6342d1328e0f0a4928ce37ba2299e2f867a1`.
Inspected on 2026-10-19 with REA 6.7.0 and JADX headless MCP 0.7.1 (engine SHA256
`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`).
Builder/guard findings below are **static only**. The separate owner-observation
sections record scoped live swing evidence from one model 140013 unit.

## Current horizontal mapping (latest owner comparison)

Use this summary in preference to superseded intermediate observations below.
Scope is one model 140013 unit, 2026-10-19, with the observer facing the AC.

| Code | Current observation |
|---|---|
| 0 | Continuous horizontal oscillation |
| 1 | Appeared centered on retest from 2; exact angle/fixedness not separately verified |
| 2 | Observed left-facing; ordering relative to 3 unverified |
| 3 | Moved slightly left from 1; earlier center label withdrawn |
| 4 | Moved observer-right from 3 |
| 5 | Moved farther observer-right from 4 and stayed fixed; rightmost tested code |

No universal full 1–5 ordering or calibrated angles are established. Model metadata
and a fresh readback remain required for each command.

## Model binding and commands

| Capability | Key on inspected AC model 140013 | Native index (`ki`) | Builder |
|---|---|---:|---|
| VerticalSwing | `acvs` | 5 | `RangeCapability` |
| HorizontalSwing | `achs` | 6 | `RangeCapability` |

Keys come from the selected model's attributes. Indexes come from
`BaseCapability.Type` in [the registry](capability-registry.json), not array order.
Do not reuse these mappings for IR/partner devices or other models without evidence.
The inspected model advertises integer bounds 0–5 for both axes. These numbers
are position codes, not degrees. Only the model-scoped observations below
establish physical behavior; relative horizontal ordering of 2/3 and exact angles remain unverified.

Use the [platform control envelope](platform-api.md), for example numeric vertical
position 3 (only when the actual model advertises it):

```json
{"deviceId":"<selected-device-id>","control_params":{"cnt":"an","sid":"0","acvs":3,"ki":5}}
```

The shipped AC setters select the corresponding advertised range capability and
send its `setNewValue(int)` result. `RangeCapability.buildMessage(int, JsonObject)`
and `buildMessage(int)` add the model key with an **integer** and the capability's `ki`; no extra swing
companion fields were observed in those setters. This establishes construction,
not server acceptance or physical movement.

## Guards and values

`SwingControlViewHolder.onViewClick` ignores ordinary device-control clicks while
power is off or a previous request is pending. Its capability-enabled check also
requires evaporator cleaning off; its UI considers device connectivity. Preserve
these constraints and stop on offline/unknown state.

The UI cycles explicit integer choices when available. Without that list it
increments the current value by one and wraps after the advertised maximum.
This supports integer steps where the model's range omits `increment`; an adapter
should still honor narrower advertised bounds/steps and reject malformed metadata.
The MCP implementation additionally restricts this known-model adapter to 0–5.

The inspected refresh/cycling/setter methods do **not** independently establish
physical direction labels. Use the following live observations for the tested
model scope only; never transfer vertical mappings to horizontal swing or other
firmware by assumption. The separate horizontal section includes its observer perspective.

## Owner-observed vertical positions — 2026-10-19

Scope: one MirAIe India model **140013 / CS-CU-NU18BKY5WX** unit, cloud REST via the
MCP server, power on, Cool, cleaning off. Firmware version was not captured during
this sequence, so applicability to other firmware is unverified.

| Code | Cloud evidence | Owner observation |
|---|---|---|
| 1 | Initial reported baseline | Absolute angle not characterized |
| 2 | Accepted and readback matched | Moved downward from 1 |
| 3 | Accepted and readback matched | Moved farther down from 2 |
| 4 | Accepted and readback matched | Moved farther down from 3 |
| 5 | Accepted and readback matched | Moved farther down from 4; stayed fixed |
| 0 | Accepted and readback matched | Oscillates continuously |

Thus **0 is vertical oscillation**, and **5 is the lowest tested fixed position**
on that unit; 1–5 form the observed downward sequence. Exact angles and a physical
end-stop calibration are unknown. Horizontal remained at its reported code and
was not actuated or mapped. Readback matching and owner observation were captured
separately; prior observations must never be represented as automatic physical
confirmation of a later command. Last requested vertical state in this sequence
was 0 (oscillating); no restoration was requested.

Publish these semantics with model/date/scope and allowed numeric values so another
agent can translate an explicit oscillation/downward request without repeating
the investigation. Validate the actual advertised schema and read back every new
command; physical observation can still be needed on another installation.

## Owner-observed horizontal positions — 2026-10-19

Same one-unit/model scope as the vertical observations. Vertical was set to 5 to
make the left/right vanes visible. Directions below are **the observer's left/right
while facing the air conditioner**, not the appliance's left/right from behind.

| Code / transition | Cloud evidence | Owner observation |
|---|---|---|
| 2 | Initial reported baseline | Physical direction not characterized |
| 2 → 3; later 0 → 3 | Both accepted and readback matched | Owner later confirmed 3 centered / straight ahead and fixed |
| 3 → 4 | Accepted and readback matched | Moved from observer left toward observer right |
| 4 → 5 | Accepted and readback matched; later status still 5 | Moved farther to observer right, then confirmed fixed |
| 5 → 0 | Accepted and readback matched | Oscillates continuously |

Initial observations labeled 3 centered, but the later direct 1→3 comparison
below supersedes that label. **Horizontal 0 oscillates**,
**5 is the rightmost tested fixed position**, and
3→4→5 is the observed rightward sequence from an observer facing the unit.
Position 1 was not tested; position 2 was reported initially but its physical
direction was not characterized. Do not infer the full 1–5 ordering or a leftmost
position. Exact angles and other firmware remain unknown. Last requested state
was horizontal 0 (oscillating) with vertical 5 (fixed down); no restoration was
requested. Temperature and display were not changed by the horizontal test.

MCP `capabilities` / `control_ac` descriptions and structured horizontal guidance
publish the perspective, per-code observations, allowed values, date and one-unit
scope. Readback for future commands must still be checked; prior physical
observations do not automatically confirm new movement.

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


### Horizontal position 2 confirmation and ambiguous position 1 — 2026-10-19

After the owner confirmed 3 centered/fixed, a requested 3→1 command was accepted
and cloud-matched. The owner reported that it still appeared centered, so this
is an ambiguous physical result, not proof that code 1 means centered. A separately
requested 1→2 command was then accepted and cloud-matched; the owner observed it
moving left and sitting left of center while facing the AC. Thus code **2 is
observed left-facing** relative to center 3 on this unit. Fixedness at 2 was not
separately confirmed. Do not call 2 the leftmost position; code 1 still needs a
controlled physical retest. Earlier notes saying 2 was uncharacterized are
superseded by this observation. Last requested horizontal code is 2.


### Latest correction: direct horizontal 1→3 comparison — 2026-10-19

A separately authorized 2→1 retest was accepted and cloud-matched; the owner
reported that 1 appeared centered. A requested 1→3 comparison was then accepted
and cloud-matched; the owner reported 3 moved a little left. **Withdraw the earlier
3=center label**. Current observed meanings are: 1 appears centered; 2 was observed
left-facing; 3 is slightly left of 1; 4 moved right from 3; 5 moved farther right
from 4 and stayed fixed; 0 oscillates. Relative ordering of 2 versus 3, exact
angles, and fixedness of 1/2 were not separately established. Treat this as an
owner-observed correction rather than a calibrated universal position map.
MCP descriptions, capability observations and command guidance use the corrected
mapping. Last requested horizontal code is 3; vertical remains 5.
