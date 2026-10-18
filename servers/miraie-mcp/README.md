# openMirAIe MCP

A local stdio MCP server for **Panasonic MirAIe India**, using the official Python MCP SDK. This is unofficial and distinct from Panasonic Comfort Cloud. It connects without credentials and exposes setup guidance immediately. No background polling, appliance pairing, remote HTTP service or live control occurs during installation/setup.

## Support tracker

Current MCP implementation, checked **2026-10-19**. “Implemented” means the tool
exists with validation and tests; it does not establish physical behavior on every
appliance. Writes are limited to direct AC model **140013**, require usable actual
model metadata, and fail closed when choices/ranges or prerequisites are missing.
See [testing.md](testing.md) for reproducible checks and dated evidence.

| Feature | MCP support | Verification / remaining work |
|---|---|---|
| Local browser sign-in and explicit device selection | Implemented | Live on macOS; synthetic security and desktop/mobile browser checks |
| Hidden-input terminal sign-in | Implemented fallback | Live terminal walkthrough not verified |
| Session status, setup cancellation and logout | Implemented | Live tested; logout only removes this server's local session/default |
| Native credential storage | Implemented; no plaintext fallback | macOS sign-in/restart/logout exercised; Windows/Linux and native failure/lock scenarios remain unverified |
| Token refresh and expired-session recovery | Implemented | Synthetic refresh/rotation tests; live expiry/refresh remains unverified |
| Homes/spaces/devices and connectivity checks | Implemented, read-only | Live discovery/selection tested; other appliance families are not write-supported |
| Model capability inspection | Implemented, read-only | Live schema retrieval; advertised capability does not imply an implemented control |
| Reported status and room temperature | Implemented, on demand | Live cloud snapshots; preserve fractional temperature and unknown values |
| Daily energy, 1–31 days | Implemented, on demand | Seven-day kWh retrieval live tested; today can be partial |
| Display on/off | Implemented | Live on/off cloud readback; owner physically confirmed display-on |
| Target temperature, 16–30°C in steps of 1 | Implemented, model/mode gated | Live 21/26/27°C cloud readbacks; physical target verification remains separate |
| Power on/off | Implemented, schema gated | Synthetic tests; no live power test in this MCP test round |
| Auto/Cool/Dry/Fan mode | Implemented, schema gated | Synthetic tests; Heat unsupported; no live MCP mode test in this round |
| Fan Auto/Quiet/Low/Medium/High | Implemented, schema/mode gated | Synthetic tests; settings are not measured RPM; no live MCP fan test in this round |
| Eco, including 26°C enable sequence | Implemented, Cool/cleaning gated | Synthetic tests and partial-step reporting; live MCP verification outstanding |
| Powerful and fixed capacity (0, 40–110 advertised steps) | Implemented, schema/interlock gated | Synthetic mutual-exclusion tests; live MCP verification outstanding |
| Vertical/horizontal swing | Implemented as `vertical_swing` / `horizontal_swing`, numeric positions | Static command builders verified; 0–5 constrained by actual model metadata. Vertical 0/2/3/4/5 cloud readbacks and owner observations recorded: 0 oscillates; increasing 1–5 moves down, 5 is the lowest tested fixed position. Horizontal 0 oscillates, 1 appeared centered on retest, 2 points observer-left, 3 moved slightly left from 1, 3→4→5 moves right facing the AC, and 5 stays fixed; relative order of horizontal 2/3 and exact angles remain unverified |
| Cleaning / sleep-profile / JetStream actions | **Unsupported** | Known states gate dependent controls; no action tools implemented |
| Timers and schedules | **Unsupported** | Timer metadata can be inspected; no command or scheduler implemented |
| Other models, appliance families and partner/parent-routed writes | **Unsupported** | Discovery/schema inspection only; status normalization remains AC-specific |
| LAN, BLE, IR, Matter and MQTT control/streaming | **Unsupported** | Current server uses cloud REST and local stdio only |
| Continuous polling, history recording and CSV export | **Unsupported** | Snapshot/energy tools are on demand; no background collection or export tool |
| Humidity, measured fan RPM and instantaneous watts | **Unsupported** | No invented readings or conversion from fan setting/Daily energy |

Maintain this table when tools, model policies or evidence change. Keep implemented,
inspection-only, unsupported and live-unverified behavior distinct.

## First run

Prerequisites: Python **3.11+**, [uv](https://docs.astral.sh/uv/getting-started/installation/), a local desktop browser and an unlocked OS credential store. From the repository root:

```sh
cd servers/miraie-mcp
uv sync --frozen
uv run --frozen --no-sync miraie-mcp config
```

The first command explicitly downloads the pinned dependencies in `uv.lock`. The second prints a complete `mcpServers` configuration with this installation's **absolute Python path**. Copy its `miraie` entry into your MCP client's server configuration and restart that client. It contains no account details and requires no working-directory setting. Do not launch through an unpinned `uvx` package or put credentials in client configuration.

Equivalent example (replace the path; on Windows use `.venv/Scripts/python.exe`):

```json
{
  "mcpServers": {
    "miraie": {
      "command": "/absolute/path/to/repo/servers/miraie-mcp/.venv/bin/python",
      "args": ["-m", "miraie_mcp.server"]
    }
  }
}
```

In your MCP client:

1. Ask it to call `session_status`. A fresh installation reports `unconfigured`; initialization and tool discovery already work.
2. Ask it to call `begin_setup`. Enter your existing MirAIe India email/mobile and password **directly in the local browser page**, never in chat. Phone formatting is preserved. This uses the evidenced Panasonic password-login endpoint, not invented browser OAuth.
3. After sign-in, choose a home/space/appliance from the page. Even one device requires explicit selection. The page identifies limited versus unsupported control support. Choose **Read reported status** for an optional read-only connection check, then **Finish setup**.
4. Back in the client, request `reported_status` or `capabilities`. Request an appliance command only when you actually want that change.

The browser page guides **Sign in → Choose device → Ready**, with a visible five-minute session countdown, responsive device cards, optional reported readings and explicit recovery screens. Password fields are cleared after errors; successful submissions redirect so refreshing cannot resend credentials. The page uses bundled styles/scripts with a hash-restricted content security policy and no remote assets.

The browser page expires after five minutes. Closing the tab alone leaves the short-lived listener until expiry; `cancel_setup` closes it earlier. Restart with `begin_setup`. If a browser cannot open (including a remote/headless MCP host), use the local hidden-input fallback:

```sh
uv run --frozen --no-sync miraie-mcp login
```

It requires an interactive terminal, never accepts a password argument, and lists numbered choices. Cancellation after successful login retains the session but does not select an arbitrary device. Use `logout` to remove the saved session. A completed sign-in with no devices gives recovery guidance; setup never pairs or controls hardware.

## Tools and resource

| Surface | Behavior |
|---|---|
| `session_status`, `miraie://onboarding` | Local configured/unconfigured/expired/storage state and exact next action; network is explicitly unchecked |
| `begin_setup`, `cancel_setup` | Open/close this process's protected loopback setup page; no credential parameters |
| `logout` | Remove only this server's session/default device; other apps and appliance data are untouched |
| `connection_test` | Explicit read-only discovery/authentication test; offline/service/session errors are distinct |
| `discover_devices` | Human-readable home/space/device choices, opaque device handles, model/category and parent/service markers |
| `select_default_device` | Explicit local default selection; validates discovery membership and inspects its model |
| `capabilities` | Safe model capability/attribute schemas, including unimplemented capabilities; unsupported families remain read-only |
| `reported_status` | One cloud snapshot, observed timestamp, reported versus requested state, fractional room temperature and fan **setting**, never RPM |
| `daily_energy` | On-demand 1–31 days, Daily kWh strings preserve decimal precision; today can be partial |
| `control_ac` | One requested action: power, mode, temperature, fan, display, Eco, Powerful, capacity or either swing axis; bounded readback and partial results |

`device` is an opaque handle from discovery; omitting it uses only an explicitly selected default. Arbitrary IDs and stale selections are rejected. General discovery/model inspection is available; status normalization and energy semantics are AC-specific. Unsupported status shapes fail honestly rather than being relabeled as AC telemetry. Private identifiers, topic strings, registration/UDP secrets and arbitrary raw metadata are not exported. Unknown capability names/attribute schemas remain inspectable.

## AC behavior and evidence limits

Writes are restricted to direct MirAIe AC model **140013 / CS-CU-NU18BKY5WX**, with actual model attributes and advertised values checked on every operation. Partner/parent-routed devices and other models are read-only. Missing or ambiguous schemas fail closed. Capability indexes follow the documented native builders, not array positions.

- Power (`PowerStatus`, index 1) and display (`DisplayControl`, index 8) are independent. Power alone does not alter the target.
- Modes: Auto/Cool/Dry/Fan; no Heat. Temperature uses advertised Celsius bounds/step within 16–30°C; fan choices are Auto/Quiet/Low/Medium/High.
- Power must be on for settings. Send mode first, wait for a matching readback, then request dependent settings separately. Fan mode has no temperature setting; Dry never sends an extra Low-fan command.
- Cleaning blocks temperature/fan/Powerful, including Eco's temperature side effect. Eco requires Cool, confirms Eco on, then requests 26°C. Powerful requires Cool and first disables fixed capacity. Nonzero capacity first disables Powerful. Values: 0 (Normal), 40/50/60/70/80/90/100/110.
- Availability recognizes explicit `online`/`offline`, boolean `true`/`false`, numeric 1/0 and Panasonic's string `"true"`/`"false"`; raw reported values are preserved. Unknown/offline availability blocks writes. Active sleep-profile reset and JetStream cancellation are unsupported and gated; cleaning, schedules/timers, LAN/BLE/MQTT and other appliance writes are not exposed.

Each mutation is sent **once**, followed by at most three status reads (3/1.2/1.8-second waits, with the first read at least three seconds after each change). Results distinguish server acceptance, matching cloud report, uncertain acceptance, remaining steps and physical confirmation (always false here). A dependent step stops after unconfirmed readback, an error, a changed session or incompatible reported state. No automatic mutation retry or queued replay after reconnect/login occurs. A timeout can mean the command reached Panasonic: inspect status before requesting anything again.

This server has synthetic coverage plus scoped live verification on one model 140013 installation: onboarding, discovery, status, Daily energy, selection/logout, and display/temperature/both-swing-axis cloud readback. Display-on and scoped vertical/horizontal swing behavior were physically observed by the owner. See [testing.md](testing.md) for per-operation results and remaining gaps; the [canonical skill](../../skills/panasonic-miraie/SKILL.md) separately records prior protocol evidence. Other controls, token refresh and broader firmware/platform behavior are not established by these tests. Refresh uses the evidenced `tokenRefresh` envelope, supports rotated tokens and retries only a read once after 401/406. The prototype's 406 interpretation and refresh success remain live-unverified. Expired/failed sessions give explicit recovery. Writes never resume across refresh.

### Swing usage and limits

`control_ac` accepts `vertical_swing` or `horizontal_swing` with an **integer**
`value` returned under `capabilities.swing.<action>.positions`. For example,
`{"action":"vertical_swing","value":3}` requests numeric position 3, only when
advertised by the selected model. Missing/ambiguous/out-of-scope range metadata,
fractional values, power off, cleaning, offline and unknown availability block
writes. Neither axis changes power, mode, temperature or the other axis.

These are raw position codes, **not angles**. On one owner-observed model 140013
unit, the following mapping was established on 2026-10-19:

| Vertical code | Observed behavior |
|---|---|
| 0 | Continuous oscillation |
| 1 | Starting position; absolute angle not verified |
| 2 | Downward from 1 |
| 3 | Farther downward from 2 |
| 4 | Farther downward from 3 |
| 5 | Farther downward from 4; fixed, lowest tested position |

Agents receive these observations through `capabilities.swing.vertical_swing`,
the `capabilities` and `control_ac` tool descriptions, and `swing_guidance` in
swing command results. Metadata is limited to positions advertised by the actual
model. Horizontal observations use the perspective of an observer **facing the
AC** (observer left/right):

| Horizontal code | Observed behavior |
|---|---|
| 0 | Continuous horizontal oscillation |
| 1 | Appeared centered / straight ahead on owner retest from 2; exact angle/fixedness not separately established |
| 2 | Left of center while facing the AC; owner observed |
| 3 | Moved slightly left from centered 1 on direct comparison; earlier center label withdrawn |
| 4 | Moved right from 3 |
| 5 | Moved farther right from 4; fixed, rightmost tested position |

The same MCP descriptions, `capabilities.swing.horizontal_swing` and command-result
`swing_guidance` expose these observations and the perspective. Other units/firmware,
exact angles, and the relative ordering of horizontal 2/3 remain unverified. The latest 1→3
comparison supersedes the earlier center label for 3. Do not infer that all codes 1–5
progress left-to-right from the tested 3–5 subset. The API still takes integers rather than string aliases. Prior
observations do not establish physical confirmation of a new command.
Each write is sent once and uses the same three-second first-read delay and
bounded readback. See [swing evidence](../../skills/panasonic-miraie/references/swing.md).

## Storage, security and recovery

Tokens and default selection occupy one `openmiraie-mcp` / `session-v1` native credential item. Passwords are never persisted. Supported adapters are macOS Keychain, Windows Credential Locker and Linux Secret Service (unlocked desktop D-Bus session required). **There is no plaintext/file fallback.** The configured native-store path worked for local sign-in, persistence across an MCP restart and logout on the tested macOS host; native store contents were not inspected. Automated tests use a synthetic backend; Windows/Linux native-store behavior remains unverified. Linux headless hosts without Secret Service cannot persist a session.

Only lock files are created in the OS per-user state directory (typically macOS `~/Library/Application Support/openmiraie-mcp`, Linux `~/.local/state/openmiraie-mcp`, Windows per-user local app data). POSIX directory/file permissions are 0700/0600. Nothing secret is written into the repository. Shared locks serialize login/refresh/control across MCP processes, prevent duplicate setup windows, and are released by the OS after a crash; restart does not replay work.

The browser listener binds only `127.0.0.1` on a random port. A one-use launch nonce is opened locally but never returned by a tool. Exact Host/Origin checks, an HttpOnly SameSite=Strict cookie, rotating CSRF tokens, bounded bodies/lifetime, no CORS, no external page assets and no request logging protect setup. As with other local applications, a compromised same-user host/browser can access local secrets; this is not a remote multi-user authentication service. Network requests use TLS validation, fixed Panasonic hosts and no redirects or ambient proxy credentials.

| Condition | Recovery |
|---|---|
| Wrong credentials | Retry directly in the browser; upstream bodies and posted fields are never echoed |
| Offline/service unavailable | Keep the existing session; run `connection_test` after connectivity recovers |
| Expired/rejected session | Run `begin_setup`; commands are not replayed |
| Credential store unavailable/locked | Unlock/configure the native store, run doctor, then restart setup; no insecure fallback |
| No devices / unsupported appliance | Check the official MirAIe India account; inspect model capabilities; no speculative writes |
| Another process busy | Complete/cancel its setup or operation; retry afterward |
| Setup cancelled/restarted | Any completed login remains; `logout` removes it and the default selection |

## Build, doctor and tests

See [testing.md](testing.md) for the complete tool checklist, automated coverage, live-test procedures and dated results.

After the explicit dependency install, from the repository root:

```sh
python3 scripts/project.py doctor miraie-mcp
python3 scripts/project.py build miraie-mcp
python3 scripts/project.py test miraie-mcp
```

The root doctor checks toolchain/environment availability without touching account storage. Build/test resolve dependencies offline and output the wheel/sdist under ignored `dist/`. To inspect local storage/session state, run `uv run --frozen --no-sync miraie-mcp doctor` in this directory. It does not contact Panasonic; use `connection_test` for an explicit network check.

Tests cover real stdio initialization/tool/resource requests, missing credentials, local browser security and lifecycle, explicit selection, wrong credentials, no devices, native-store failure, cross-process locking, refresh envelopes/rotation, no mutation replay, redaction, AC guards and accepted-but-unconfirmed/partial results. All use synthetic accounts and transport fixtures. macOS and Linux CI are configured; Windows runtime, Windows/Linux native adapters and native-store failure/lock scenarios still require platform verification. No live account, phone or appliance is needed.

Protocol sources: [MirAIe platform API](../../skills/panasonic-miraie/references/platform-api.md), [AC behavior](../../skills/panasonic-miraie/references/air-conditioners.md), [official MCP Python SDK](https://github.com/modelcontextprotocol/python-sdk), [MCP server guide](https://modelcontextprotocol.io/docs/develop/build-server), [keyring backends](https://keyring.readthedocs.io/en/latest/). `uv.lock` pins SDK 2.3.0 and all transitive dependencies; the installed SDK is exercised by protocol tests rather than a handwritten JSON-RPC loop.

### Browser regression checks

HTTP fixtures alone do not establish browser form behavior. Install the test browser explicitly, then run the real Chromium suite:

```sh
uv run --frozen --no-sync python -m playwright install chromium
MIRAIE_BROWSER_TESTS=1 uv run --frozen --offline --no-sync pytest -q
```

Browser tests use synthetic accounts and an in-memory credential backend. They cover desktop and mobile layouts, the complete sign-in/select/read/finish flow, wrong-password recovery, empty discovery, refresh without resubmission, and browser-generated Origin headers. Without `MIRAIE_BROWSER_TESTS=1`, these tests are explicitly skipped; CI installs Chromium and enables them. Playwright is a development-only dependency.

The original login form returned a local 403 because `Referrer-Policy: no-referrer` caused an actual browser POST to use `Origin: null`. The corrected page uses `same-origin`; exact Host/Origin, cookie and CSRF checks remain enforced. A browser regression deliberately restores the old policy and verifies rejection before any Panasonic request. Native HTTP fixture tests retain missing/stale/cross-origin rejection checks.
