# Panasonic MirAIe integration

A monorepo for an unofficial, portable Panasonic MirAIe integration skill and native apps built around verified protocol behavior. The existing Room AC Android implementation is a **proof of concept**. The new **openMirAIe** native Android app has a buildable first iteration; native iOS and macOS apps are planned.

The protocol snapshot comes from MirAIe India Android 01.4.32 and live testing one AC model, CS-CU-NU18BKY5WX. This project is not affiliated with or endorsed by Panasonic. Other device families are research leads rather than tested integrations.

Read our [philosophy](philosophy.md): reusable integration skills, lightweight open implementations, customization, and user-controlled device-data recording and observability.

## Repository map

| Location | Responsibility |
|---|---|
| [skills/panasonic-miraie](skills/panasonic-miraie/SKILL.md) | Self-contained, installable integration skill and authoritative protocol/evidence references |
| [apps/openmiraie](apps/openmiraie/README.md) | New production native app boundaries: Android, iOS and macOS |
| [prototypes/room-ac-android](prototypes/room-ac-android/README.md) | Working Android PoC retained for protocol and behavioral verification |
| [design/openmiraie](design/openmiraie/README.md) | Maintained interaction prototype, user flows, design system and past explorations |
| [tools/stay-awake-android](tools/stay-awake-android/README.md) | Optional investigation helper using silent local video in PiP |
| [servers/miraie-mcp](servers/miraie-mcp/README.md) | Local MCP server with secure account setup, read-only discovery and model-aware AC controls |
| [packages](packages/README.md) | Policy for future shared libraries; none extracted yet |
| [docs](docs/architecture.md) | Architecture, development and contribution guidance |
| [projects.json](projects.json) | Explicit target lifecycle and build/test commands |

The skill includes 209 static API declarations, 231 capability-registry records and 359 serialized field schemas. It distinguishes **live verified**, **static only**, **inferred** and **unknown** behavior. Keep `skills/panasonic-miraie/` intact when installing it; the skill does not depend on this repo's apps, private investigation files or build tools.

## Start here

Run from the repository root with Python 3.10+:

```sh
python3 scripts/project.py list
python3 scripts/project.py check
python3 scripts/project.py doctor android-poc
python3 scripts/project.py build android-poc
python3 scripts/project.py test android-poc
python3 scripts/project.py build stay-awake
```

`check` needs Node.js 22+ for prototype checks, but no Android SDK, Xcode, account or phone. PoC/helper builds need a JDK and Android SDK; see [development setup](docs/development.md). Build commands never install on a phone, log in or deploy. Production Gradle builds resolve pinned dependencies on the first run.

Production target `android` is active: `python3 scripts/project.py build android` builds the independent native app and `test android` runs its synthetic JVM tests. Targets `ios` and `macos` remain **not buildable yet**. No target falls back to the PoC. See the [Android implementation and verification gaps](apps/openmiraie/android/README.md).

## Design and implementation

Open the [current clickable prototype](design/openmiraie/prototype/index.html), [journey map](design/openmiraie/prototype/map.html) and [full interaction handoff](design/openmiraie/flows/user-flows.md). The prototype uses synthetic data and performs no real AC requests. Production apps will implement the approved behavior natively; the prototype is not a web runtime to embed in the apps.

The PoC demonstrates capability-driven modes, fan, Eco/Powerful/capacity, independent AC display control, manual widgets, local snapshots and separate CSV exports. It is an integration reference, not the foundation to rename as the finished production app. Existing native protocol logic can inform new implementations after review.

## Validation and publication

The CI workflow checks repository structure, the portable skill, documentation links, prototype behavior and build orchestration. Android CI now builds, tests and lints the native project; Apple CI remains deferred. Local PoC tests and live device verification are distinct from CI; see [development](docs/development.md) and [evidence/extension guidance](skills/panasonic-miraie/references/evidence-and-extension.md).

Private captures, account/device identifiers, sensor-history exports, synced references, APKs, credentials and signing material stay excluded. `analysis/`, `sources/` and `.stitch/` are local working/reference areas, not public source. Sanitized skill catalogs contain protocol declarations, not account responses. Preserve actual reported state and verify each new model before claiming support.
