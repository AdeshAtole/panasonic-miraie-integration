# MirAIe MCP testing

This guide owns repeatable verification procedures and sanitized results for this
component. Read [AGENTS.md](AGENTS.md), [README.md](README.md), and the
[protocol skill](../../skills/panasonic-miraie/SKILL.md) first. Tests do not grant
permission for future live sign-in, appliance changes or logout.

## Evidence levels

- **Synthetic pass:** exercised against fake transport/account/storage, not hardware.
- **Live readback pass:** actual MCP tool call and matching cloud response.
- **Physical confirmation:** the owner explicitly observed the requested effect.
- **Not verified:** no claim about real accounts, platforms or physical behavior.

Do not call an accepted command successful until readback matches. A cloud match
still does not prove physical actuation. Record delayed confirmation separately.

## Automated checks

From this component directory, install pinned dependencies explicitly:

```sh
uv sync --frozen
uv run --frozen --offline --no-sync pytest -q
```

For the complete suite, install Chromium and enable the browser checks:

```sh
uv run --frozen --no-sync python -m playwright install chromium
MIRAIE_BROWSER_TESTS=1 uv run --frozen --offline --no-sync pytest -q
uv build --offline --no-sources
```

Browser tests require local loopback access. If browser binaries were installed in
a custom location, set `PLAYWRIGHT_BROWSERS_PATH` to that installation directory.
Without `MIRAIE_BROWSER_TESTS=1`, browser tests are skipped; report this explicitly.
No test needs a live account, OS keychain entry or real appliance.

From the repository root:

```sh
python3 scripts/project.py doctor miraie-mcp
python3 scripts/project.py build miraie-mcp
python3 scripts/project.py test miraie-mcp
python3 scripts/project.py check
```

### Test ownership

| File | What it establishes |
|---|---|
| `tests/test_tools.py` | Exact 11-tool inventory; SDK calls to every tool; all ten control actions; safe errors; setup cancellation; logout/default cleanup; onboarding resource before/after logout |
| `tests/test_protocol.py` | Actual stdio initialization, tool/resource discovery, unauthenticated errors, annotations and three-second tool guidance |
| `tests/test_setup.py` | Host/Origin/cookie/CSRF and one-time forms, lifetime, locking, cancellation/restart, safe login errors |
| `tests/test_browser.py` | Real Chromium desktop/mobile forms, success/error/empty states, no resubmission, original null-Origin regression |
| `tests/test_session.py` | Synthetic credential storage, refresh/rotation, network/auth failures, redaction, cross-process locking |
| `tests/test_swing.py` | Both axes/indexes, 0–5 integer payloads, model-derived keys/ranges, power/availability/cleaning guards, safe rejection and unconfirmed send-once readback |
| `tests/test_domain.py` | Model-derived commands, ranges/interlocks, availability, partial/unconfirmed results, three-second first readback, telemetry and energy |

Keep inventory coverage exact: adding/removing a registered tool must update
`TOOL_NAMES` in `test_tools.py` and the matrix below. All ten control actions run
through the official SDK as well as relevant domain-policy tests.

## Live tool checklist

Use the connected MCP tools, starting with `session_status`. Capture a baseline
without copying account/device identifiers into this file. Use opaque handles only
inside the active tool session. Never put credentials, tokens, local nonce URLs,
private account labels or full raw responses in fixtures or reports.

| Tool | Procedure and expected result | Live result, 2026-10-19 |
|---|---|---|
| `session_status` | Configured session reports explicit default; after logout it reports unconfigured and no default | Pass, both states |
| `connection_test` | Authenticated read-only discovery succeeds; signed-out use returns safe recovery error | Pass, both states |
| `discover_devices` | Inspect hierarchy/model/support without automatically choosing a default | Pass, one AC discovered |
| `select_default_device` | Explicitly reselect a discovered handle; confirm model/support; never invent an ID | Pass, existing selection retained |
| `capabilities` | Read model choices/ranges and support boundaries; no actuation | Pass, model 140013 inspected |
| `reported_status` | Read a timestamped cloud snapshot; preserve raw fields; room temperature is not target temperature, fan setting is not RPM | Pass; online string availability normalized |
| `daily_energy` | Request 7 days; verify Daily kWh and today's partial marker; do not label energy as instantaneous power | Pass, seven entries returned |
| `begin_setup` | Open protected local page with bounded lifetime; no credentials in tool arguments/results | Pass, five-minute page opened; earlier live sign-in also completed |
| `cancel_setup` | Cancel the opened page; confirm saved session/default remain usable | Pass, retained configured session |
| `control_ac` | Follow the control matrix and restoration procedure below | Display/temperature passed; other six actions synthetic only |
| `logout` | Run last when the user agrees to sign in again; confirm only server session/default removed and read tools fail safely | Pass; final state unconfigured/default absent |

The `miraie://onboarding` resource is additionally checked through the SDK and
stdio tests. A separate live resource read was not performed in this run.

## Control test matrix

The user authorized live display/temperature tests for this run and synthetic tests
for all other actions. This is a scope record, not permission for later runs.

| Action | Synthetic coverage | Live evidence on model 140013 |
|---|---|---|
| `display` | On/off, independent power/index/key, availability gates, SDK call | Off → on → off accepted and cloud-matched. Earlier display-on was physically confirmed by owner; this test round has cloud confirmation only |
| `temperature` | Valid range/step, cleaning/power/mode guards, SDK call, first-read delay | 26 → 27 → 26°C accepted and cloud-matched; a subsequent user-requested 21°C change also cloud-matched. Earlier 26°C request confirmed on a later read after initial unconfirmed result |
| `power` | SDK off, independence, safe guards and uncertain acceptance | Not tested live in this run |
| `mode` | SDK Dry, no extra Low-fan command, invalid Heat blocked | Not tested live in this run |
| `fan` | SDK High, unsupported choices/modes/cleaning blocked | Not tested live in this run |
| `eco` | SDK enable plus 26°C sequence, preflight and stopped dependent steps | Not tested live in this run |
| `powerful` | SDK enable, capacity exclusion, Cool/cleaning guards | Not tested live in this run |
| `capacity` | SDK 80, Powerful exclusion and supported values | Not tested live in this run |

### Live control and restoration procedure

1. Agree on the exact actions and logout scope. Read current status/capabilities.
   Snapshot relevant settings in memory; verify online availability and model.
2. Choose a reversible small change. This run used display off/on and 26/27°C.
   Call `control_ac` once. The server waits at least three seconds before its
   first readback; callers must also wait at least three seconds before any
   separate status check after a change. Restart the server to load code changes.
3. If accepted but unconfirmed, wait and read status; do not resend automatically.
   Do not continue dependent changes until confirmed. Stop on incompatible external
   changes, session loss, offline status or ambiguous outcomes.
4. Restore the confirmed baseline with explicit commands, checking each result.
   If restoration is unconfirmed, report the outstanding setting before logout.
   Do not blindly force a stale baseline over changes made by someone else.
5. Read final status. If requested, test logout last, then `session_status` and
   `connection_test`. The owner signs back in through a fresh local page.

Final cloud baseline was restored before logout: power on, Cool, 26°C, Low fan,
display off, Eco off, Powerful off, normal capacity (0). No live fan/mode/power/
Eco/Powerful/capacity command was sent during this run.

## Initial complete-tool verification record

Date: **2026-10-19 (Asia/Kolkata)**. Host: macOS, Python 3.12.

- **81 automated tests passed**, with Chromium browser checks enabled. Includes
  ten new SDK tests covering the full tool surface, eight control actions and
  safe input errors; the earlier 71 checks remain passing.
- Wheel and source distribution built successfully offline; repository checks and
  component documentation links/whitespace also passed. CI execution itself was
  not verified in this local run.
- All **11 MCP tools** were called live within the agreed scope. Error behavior
  after logout was also checked. Tool coverage is not exhaustive value/hardware
  coverage; only display and temperature were actuated live in this run.
- Real local onboarding/session persistence worked on this macOS installation,
  survived an MCP restart, and logout removed the session/default. Credential
  material and native store contents were never inspected/exported.
- Live refresh/expiry, credential-store locking/failure, offline hardware behavior,
  other models/firmware and Windows/Linux native-store integration remain
  unverified; synthetic checks do not close these gaps.
- The capability result's evidence text must remain conservative and point to this
  per-operation record; do not infer whole-model or cross-platform verification.

When repeating tests, update the date, actual counts, environment failures/skips,
per-tool/control outcomes and final restoration/session state. Never record a
planned test as passed or copy private responses into this document.

### Subsequent requested operation

After the logout test, the owner signed in again through the local setup page.
User-requested 21°C and display-on commands were accepted and cloud-matched.
These were requested settings, not a test restoration; they supersede the earlier
test baseline/session state. No swing command was sent.

## Swing implementation verification — 2026-10-19

- **148 automated tests passed**, including real Chromium desktop/mobile checks,
  SDK calls for all ten control actions and protocol discovery of both new actions.
- Offline wheel/source build, repository checks, documentation links and diff
  whitespace checks passed. CI execution and live swing remain unverified.
- New actions: `vertical_swing` (`VerticalSwing`, index 5) and `horizontal_swing`
  (`HorizontalSwing`, index 6), using the selected model's attribute keys.
- Commands use integer JSON values, a model-advertised range/list within 0–5,
  power-on/cleaning-off/online guards, send-once behavior and three-second first
  readback. Invalid direction strings, floats, booleans, missing/ambiguous schemas
  and unsafe states are rejected without a write.
- `capabilities.swing` exposes allowed numeric positions with static-only evidence
  and explicitly unverified physical directions. Malformed swing schemas are
  omitted from `writes` while their metadata remains inspectable.
- Basis: [static swing evidence](../../skills/panasonic-miraie/references/swing.md)
  from MirAIe Android 01.4.32. No live swing command was sent during implementation.
- The earlier eight-action live-test scope and results above remain historical;
  power/mode/fan/Eco/Powerful/capacity and both swing axes have no new live test here.

For a future owner-authorized swing test, read the baseline code for the selected
axis, choose one advertised code explicitly, send once, wait/read until confirmed
within bounds, and ask the owner what position it produced. Restore the original
code only after confirmation. Treat cloud readback and physical direction as
separate evidence. Do not sweep codes or assume numeric order means down/up.


## Owner-observed vertical mapping and MCP guidance — 2026-10-19

The owner authorized sequential position changes and reported the physical result.
Commands for vertical 2, 3, 4, 5 and 0 were each accepted and cloud-matched. From
baseline 1, the owner observed 2 moving downward, 3 farther down, 4 farther down,
and corrected/confirmed 5 moving farther down again and staying fixed. At 0 the
owner observed continuous oscillation. No exact angles or firmware version were
recorded. Horizontal swing was not changed or physically mapped.

The MCP `capabilities` and `control_ac` descriptions now summarize this one-unit
model 140013 mapping. Structured `capabilities.swing.vertical_swing` includes
allowed positions, per-code observations, date and scope, with `direction_mapping`
marked partially verified. Swing command results also include `swing_guidance`;
`physical_confirmation` remains false for each new command until the owner observes
it. Horizontal guidance stays static-only/unverified. Schema-restricted choices
omit observations for values the selected model does not advertise.

**150 automated tests passed**, including real Chromium checks, stdio-discovered
mapping descriptions, SDK-delivered observations and command-result guidance.
The earlier static-only swing record above describes implementation-time evidence;
this owner-observed record supersedes its no-live-swing limit for vertical only.
Last requested vertical position was 0 (oscillating); no baseline restoration was
requested. The AC temperature/display settings were not changed in this sequence.


## Owner-observed horizontal mapping and MCP guidance — 2026-10-19

The owner authorized the horizontal sequence and reported movement while facing
the AC. From initial reported horizontal 2, the 3 command cloud-matched but no
physical direction was reported. The 3→4 transition cloud-matched and moved right
from that observer perspective. The 4→5 transition cloud-matched and moved farther
right; a subsequent status still reported 5, and the owner confirmed it stayed
fixed. The 5→0 transition cloud-matched and the owner confirmed oscillation.
Vertical was explicitly set to 5 to make the horizontal vanes visible.

`capabilities.swing.horizontal_swing`, both relevant tool descriptions, and swing
command results now expose: 0 oscillates; 3→4→5 moves observer-right while facing
the AC; 5 is the rightmost tested fixed position. Structured guidance includes
`observer_perspective`, date, one-unit/model scope and per-position observations
filtered to actual advertised choices. Horizontal 1/2, the full 1–5 ordering,
leftmost direction, other firmware and exact angles remain unverified. Prior
observations never set `physical_confirmation` true on a new tool result.

**152 automated tests passed**, including Chromium browser flows, stdio-discovered
descriptions and SDK exposure of horizontal guidance, physical-confirmation
separation and model-restricted observation filtering for both axes. These live
results supersede the earlier horizontal static-only limit. Last requested state
is horizontal 0 oscillating, vertical 5 fixed down; no restoration was requested.
The documentation update itself sent no appliance command.


### Center position confirmation — 2026-10-19

A subsequent owner-requested horizontal 0→3 command was accepted and cloud-matched.
The owner confirmed that 3 looks centered / straight ahead and fixed. MCP tool
descriptions, structured observations and command guidance now publish that meaning
within the same one-unit model scope. Exact angle remains unknown. The next test
command, horizontal 3→1, was accepted and cloud-matched; its physical direction
is pending owner observation. Position 2 also remains physically unverified.


### Horizontal position 2 and ambiguous position 1 — 2026-10-19

Code 1 cloud-matched, but the owner reported no clear movement from center.
Following the owner's explicit request for another command, code 2 was sent once,
accepted and cloud-matched. The owner confirmed leftward movement and a left-facing
position while facing the AC. MCP guidance now labels 2 observer-left relative
to centered 3; code 1 remains physically unconfirmed rather than labeled centered
or leftmost. Fixedness at 2 has not been separately confirmed. This supersedes the
previous 1/2 untested wording for 2 only. Last requested horizontal state is 2;
vertical remains 5. No further appliance command accompanied this guidance update.


### Mapping correction from direct horizontal 1→3 comparison — 2026-10-19

The owner-requested 2→1 retest cloud-matched and the owner reported 1 appeared
centered. The next requested 1→3 comparison cloud-matched and moved a little left
according to the owner. This supersedes the earlier 3=center inference. MCP tool
descriptions, structured capability observations and command guidance now use
1=appeared centered, 3=slightly left of 1. Code 2 remains observed left-facing;
its order relative to 3 is unknown. Fixedness at 1/2 and exact angles remain
unverified. No new appliance command was sent to update this mapping; horizontal
remains at the user-requested code 3, vertical at 5.


### Final user-requested settings after mapping tests — 2026-10-19

The owner subsequently requested horizontal 1 and vertical 1. Both commands were
accepted and cloud-matched. These explicit requested settings supersede the earlier
test-ending codes; they are not physical confirmation or baseline restoration.
Publication preparation did not sign in, read credentials, or send further appliance
commands.
