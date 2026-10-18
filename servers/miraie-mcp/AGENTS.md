# MirAIe MCP server instructions

## Scope and context

This folder owns the independent Python stdio MCP server and local onboarding UI.
Follow the [shared repository instructions](../../docs/agent-instructions.md) and
the repository philosophy when available in the checkout. Read [README.md](README.md) for installation,
transport, storage and the support tracker; keep its table current when support changes. Read [testing.md](testing.md) before
verification; keep its tool matrix and dated evidence current when tests change.
The [Panasonic MirAIe skill](../../skills/panasonic-miraie/SKILL.md) and its references
own protocol evidence and model support boundaries.

## Layout and conventions

- `src/miraie_mcp/server.py`: official MCP SDK tools/resource, CLI and safe errors.
- `core.py`: HTTP/session handling, native credential storage and process locks.
- `domain.py`: capability validation, AC command planning and reported state.
- `onboarding.py` / `setup_ui.py`: protected loopback setup and bundled browser UI.
- `tests/`: synthetic fixtures, SDK/stdio, session/domain and real-browser checks.

Use Python 3.11+, pinned dependencies in `pyproject.toml` / `uv.lock`, and the
existing modules rather than introducing another transport or UI framework.
Keep JSON-RPC stdout clean; log only safe diagnostics to stderr. Derive controls
from the selected model. Preserve raw reported values alongside normalized fields.
Use one availability normalizer for status and every actuation guard. Swing actions
accept only advertised integer positions; require power on and cleaning off.
Expose the model-scoped owner-observed vertical mapping in MCP descriptions,
capabilities and command results: 0 oscillates; 1–5 progress downward; 5 is the
lowest tested fixed position on one model 140013 unit. For horizontal on that
unit, 0 oscillates; 2 points observer-left; 1 appeared centered on retest; 3 moved slightly left from 1 (earlier center label withdrawn); 3→4→5 moves right while the observer faces the AC; 5 stays
fixed and is the rightmost tested position. Always include the observer perspective.
Keep the relative horizontal order of 2/3, exact angles and other firmware unverified. Never mark a new command physically
confirmed just because prior observations exist.
Send a command once; wait at least three seconds before the first readback.
Keep acceptance, reported matching and physical confirmation distinct. Stop
coupled commands after unconfirmed results; never queue/replay appliance writes.

## Commands and verification

Run in this folder after `uv sync --frozen`:

```sh
uv run --frozen --offline --no-sync pytest -q
uv run --frozen --no-sync python -m playwright install chromium
MIRAIE_BROWSER_TESTS=1 uv run --frozen --offline --no-sync pytest -q
uv build --offline --no-sources
```

Root equivalents: `python3 scripts/project.py doctor miraie-mcp`, `build miraie-mcp`
and `test miraie-mcp`. Real browser checks need loopback access and installed
Chromium; report skips/environment failures separately. Exercise SDK tool
registration and errors as well as domain methods. Prefer targeted regressions
for behavior changes; do not claim synthetic tests establish live hardware support.

## Live tests and privacy

Use the procedures and restoration rules in [testing.md](testing.md). Call
`session_status` first. Credentials belong only in the local browser/hidden-input
CLI. Never inspect, export or fixture real tokens, identifiers or account metadata.
Live testing requires the user's authorized scope; a testing document is not
standing permission to operate hardware or log out. Snapshot settings before
writes, restore only confirmed test changes, and stop on external changes or
uncertain outcomes. Test logout last when requested. Keep live results sanitized;
record physical confirmation only when the user actually provides it.

## Guidance maintenance

Keep this file short and specific to this component; link shared rules instead of
duplicating them. Update commands and testing links when behavior changes. This
layout follows the [official Codex AGENTS.md guidance](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
and [OpenAI's init guidance](https://developers.openai.com/cookbook/examples/codex/iterating-development-workflows-with-codex).
