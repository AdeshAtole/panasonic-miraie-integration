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
| `control_ac` | Follow the control matrix and restoration procedure below | Display/temperature and both swing axes have live readbacks; other six actions synthetic only |
| `logout` | Run last when the user agrees to sign in again; confirm only server session/default removed and read tools fail safely | Pass; session/default removed during test; owner subsequently signed in again |

The `miraie://onboarding` resource is additionally checked through the SDK and
stdio tests. A separate live resource read was not performed in this run.

## Control test matrix

Live display/temperature tests, later swing mapping tests, and logout were each
authorized separately by the owner. The other six actions remained synthetic-only
in this MCP session. This is recorded evidence, not permission for later runs.

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
| `vertical_swing` | SDK calls, range/type/availability/power/cleaning guards and send-once readback | Codes 0–5 cloud-reported/command-matched; 0 oscillates, 1→5 moves downward, 5 fixed per owner; exact angles unknown |
| `horizontal_swing` | SDK calls, model-restricted observations and correction/perspective exposure | Codes 0–5 cloud-matched; 0 oscillates, 1 appeared centered, 2 left-facing, 3 slightly left of 1, 3→4→5 observer-right, 5 fixed; 2/3 ordering unknown |

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

During the initial display/temperature test, the cloud baseline was restored before logout: power on, Cool, 26°C, Low fan,
display off, Eco off, Powerful off, normal capacity (0). No live fan/mode/power/
Eco/Powerful/capacity command was sent during this session. Later swing tests and
explicit requested settings are summarized below.

## Current verification report — 2026-10-19

Host: macOS, Python 3.12. **152 automated tests passed**, including enabled Chromium
desktop/mobile checks and SDK/stdio verification of all 11 tools and ten control
actions. The exact committed checkpoint also passed 9 root build-routing tests,
repository checks, documentation links/whitespace, and offline wheel/source builds.
These were local checks; remote CI is a separate result and may be pending on a
new documentation revision.

All **11 tools** were exercised live within agreed scopes. Sign-in, discovery,
explicit selection, capabilities, cloud snapshots, seven-day Daily kWh, setup
cancellation and logout worked. Native session persistence survived an MCP restart
on the tested macOS host. Credential contents were never inspected/exported.
The onboarding resource was tested through SDK/stdio; a separate live resource
read was not performed. Tests used synthetic accounts/transports/native-store
backends and do not establish other platform or hardware behavior.

### Current swing evidence and correction

The portable [swing reference](../../skills/panasonic-miraie/references/swing.md)
owns the complete current position tables, static builder evidence and limitations.
Scope is one model 140013 unit, date above; firmware and exact angles unknown.

- Vertical: 0 oscillates; 2→3→4→5 moved progressively down from baseline 1; 5 stayed
  fixed. Code 1 was later explicitly commanded and cloud-matched, but its absolute
  angle/fixedness was not separately characterized.
- Horizontal, **observer facing the AC**: 0 oscillates; 1 appeared centered on
  retest; 2 was observed left-facing; 3 moved slightly left from 1; 4 moved right
  from 3; 5 moved farther right from 4 and stayed fixed. Fixedness at 1/2, the
  relative order of 2/3, full numeric ordering and exact angles remain unknown.
- Correction provenance: an initial 3=center observation was superseded by the
  direct 1→3 comparison. **Withdraw 3=center**. The initial unclear code 1 result
  is also superseded by its retest appearing centered. Cloud readback is recorded
  separately from the owner's observation; neither is erased or generalized.

MCP `capabilities` and `control_ac` descriptions, `capabilities.swing`, and
`swing_guidance` in command results publish the model/date/scope, partial mappings,
allowed codes and horizontal observer perspective. Positions not advertised by a
selected model are omitted from its observations. `physical_confirmation` remains
false for each new command; prior owner feedback does not confirm future movement.

### Final requested settings and restoration

The initial test restored its display/temperature baseline before the requested
logout. The owner then signed in again, explicitly requested 21°C/display-on, and
authorized swing exploration. The final explicit request was **horizontal 1 and
vertical 1**; both cloud-matched. These requested settings supersede earlier test
ending codes and were not automatically restored. The latest report preparation
sent no appliance command and does not claim a fresh physical or status reading.

### Remaining verification

Live token expiry/refresh, Windows/Linux native stores, native-store failure/lock
conditions, hardware-offline scenarios, other models/firmware and physical behavior
of the remaining six AC controls remain unverified in this MCP session. The
prototype's separate evidence does not establish those MCP paths. Humidity, RPM,
LAN/MQTT control, timers and background recording are not silently inferred.

When tests or evidence change, update this current report and the README support
tracker. Keep correction provenance concise; do not append conflicting current
mappings or claim test counts from unexecuted checks. Preserve the skill's
self-contained references and omit credentials, private identifiers and captures.
