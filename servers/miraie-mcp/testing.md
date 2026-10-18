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
| `tests/test_tools.py` | Exact 11-tool inventory; SDK calls to every tool; all eight control actions; safe errors; setup cancellation; logout/default cleanup; onboarding resource before/after logout |
| `tests/test_protocol.py` | Actual stdio initialization, tool/resource discovery, unauthenticated errors, annotations and three-second tool guidance |
| `tests/test_setup.py` | Host/Origin/cookie/CSRF and one-time forms, lifetime, locking, cancellation/restart, safe login errors |
| `tests/test_browser.py` | Real Chromium desktop/mobile forms, success/error/empty states, no resubmission, original null-Origin regression |
| `tests/test_session.py` | Synthetic credential storage, refresh/rotation, network/auth failures, redaction, cross-process locking |
| `tests/test_domain.py` | Model-derived commands, ranges/interlocks, availability, partial/unconfirmed results, three-second first readback, telemetry and energy |

Keep inventory coverage exact: adding/removing a registered tool must update
`TOOL_NAMES` in `test_tools.py` and the matrix below. All eight control actions run
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

## Latest verification record

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
