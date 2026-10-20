# Repository architecture

This is one Git monorepo for MirAIe knowledge, product design and native applications. It is not a collection of Git submodules, nor a single cross-platform app runtime.

## Boundaries

- `skills/panasonic-miraie/`: self-contained installable skill, authoritative evidence and protocol facts. It must remain useful without any app or private research directory.
- `apps/openmiraie/{android,ios,macos}/`: production platform projects. They own UI, widgets/extensions, storage adapters, credentials, lifecycle, dependencies and platform build configuration.
- `prototypes/room-ac-android/`: existing working proof of concept. It informs verified behavior but is not relabeled as production.
- `design/openmiraie/`: maintained interaction prototype, user flows and design source. It specifies product behavior; its HTML is not shipped inside native apps.
- `packages/`: actual reusable libraries when warranted. Do not force Android and Apple clients onto a common runtime. Share testable behavior/contracts first; native iOS/macOS may share a Swift package later.
- `tools/stay-awake-android/`: optional investigation helper, not an openMirAIe product feature.
- `tools/android/build.py`: development builder shared only by the legacy PoC/helper. Its Android35/build-tools35 pins never become production defaults accidentally.
- `scripts/project.py` + `projects.json`: common entry point and explicit target lifecycle, delegating to each project-owned toolchain.

- `servers/miraie-mcp/`: independent Python stdio MCP server with OS credential storage, local browser setup and model-scoped control policy. It imports no PoC or native-app runtime.

## Dependency rules

Production apps do not import PoC source, build output, private captures or the design runtime. The skill has no runtime dependency on apps or repository-root documentation. Unknown and unverified protocol behavior remains labeled; an API schema does not establish appliance support.

Shared code may depend on a tested protocol model; it must not depend on a particular app's navigation, credential store or UI toolkit. When protocol facts change, update the skill, affected implementation tests and product behavior guidance together. Keep sanitized/synthetic fixtures close to their actual consumers until there is more than one consumer, then extract a canonical shared fixture location deliberately.

## Lifecycle and builds

Registry stages are `planned`, `design`, `prototype`, `helper` and `active`. Planned/design targets cannot claim a successful build. The root CLI exits nonzero with setup guidance; it never substitutes the PoC. Only real scaffolded projects with committed build configuration, dependency locks/resolutions and meaningful tests become active.

Android production builds use a Gradle wrapper and project-pinned toolchain. Apple builds use Xcode project/workspace, committed shared schemes and platform-specific destinations. Unsigned development builds, simulator tests, device tests and signed distribution are distinct tasks. Version/release identities and signing keys are platform-specific; do not reuse the PoC package identity or development key for production by default.

## macOS menu bar app

macOS owns an AppKit status item/popover with native SwiftUI controls, a Keychain session store, bounded HTTPS client and reviewed-model command executor. There is no web runtime, MCP dependency or background cloud polling. Each command gets fresh preflight and bounded readback; unsent temperature drafts cancel on dismissal. Sources stay platform-owned pending a deliberate shared-package extraction. Offline tests cover protocol behavior, menu state, temperature input and native light/dark rendering. See the [macOS guide](../apps/openmiraie/macos/README.md) for build and acceptance boundaries.

## Migration notes

`room-ac/` moved to `prototypes/room-ac-android/`; `stay-awake/` to `tools/stay-awake-android/`. Entire directories moved, preserving ignored local APKs, build outputs and development signing files. Maintained designs moved out of `.stitch/`; prior design explorations are preserved. Existing app/phone state is unchanged. Build outputs and keys remain ignored.

`analysis/` remains private and ignored. `sources/` is a synced read-only reference location and is never moved or edited by repository tooling. `.stitch/` contains local tool exchanges and legacy-tab redirects, not canonical product files.

The production product is named **openMirAIe**, with lowercase `openmiraie` directory names. The existing Room AC PoC keeps its name, Android package identity and signing files. Historical design exports may retain the earlier working title.
