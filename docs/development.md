# Development and build process

## Common entry point

Use Python3.10+ from the repository root. Paths resolve relative to the script, so commands also work when invoked by absolute path from another directory.

```sh
python3 scripts/project.py list
python3 scripts/project.py check
python3 -m unittest discover -s scripts/tests -v
python3 scripts/project.py doctor android-poc
python3 scripts/project.py build android-poc
python3 scripts/project.py test android-poc
python3 scripts/project.py build stay-awake
```

`check` requires Node.js22+ for the dependency-free design prototype. It validates target paths/lifecycles, Python syntax, skill JSON and internal references, maintained documentation/prototype links, JS syntax and18 interaction-model checks. It never reads private research or accesses a phone/account. CI runs these checks and orchestration tests on Linux. These are not native-app build or visual/accessibility tests.

## Existing Android PoC and helper

Install/provide a JDK with `java`, `javac`, `keytool`, and Android SDK platform35 + build-tools35.0.0. Set `JAVA_HOME` and `ANDROID_SDK_ROOT` (or `ANDROID_HOME`) for your environment. Existing macOS/Homebrew and common Linux SDK paths have fallbacks. No SDK or dependency is downloaded automatically.

The development builder uses its configured SDK/JDK locations rather than requiring `/opt/homebrew` everywhere. Windows tool suffixes are resolved, but this migration is validated on the existing macOS host; Windows/Linux native builds are not claimed tested.

PoC-only overrides: `POC_ANDROID_API` and `POC_ANDROID_BUILD_TOOLS`. These do not configure future production Gradle projects. Direct per-project `python3 build.py` remains available. Output APKs remain inside each project and are ignored; intermediate artifacts and development signing key are in its ignored `build/` directory.

The migrated signing files are preserved. Do not delete `build/development.p12` if you need to update an existing PoC/helper installation. A missing key creates a new development identity. This development key/password setup is not production release signing.

PoC JVM tests need the test-only `com.vaadin.external.google:android-json:0.0.20131108.vaadin1` JAR. Set `ANDROID_JSON_JAR` to its installed path, or use the conventional Maven-cache location. The Android platform JAR and a built PoC are required. Build first, then test; dependencies are never silently fetched. No live control is part of build/test.

## New native apps

`build android`, `build ios` and `build macos` currently exit2 and explain the missing project. Their folders intentionally contain implementation guidance rather than pretend native scaffolds.

When a platform starts:

1. Add the real project inside its app directory, pin project-owned toolchain/dependencies and commit wrappers/shared schemes and appropriate lock/resolution files.
2. Update required files and build/test argument arrays in `projects.json`; activate the target only once these are meaningful. Commands run in the target directory without shell interpolation.
3. Add platform CI with its actual SDK/Xcode environment. Root repository checks remain cross-platform and independent of native SDK availability.
4. Keep build outputs in ignored `build/`, `.build/` or DerivedData. Configure release signing/distribution separately; never commit credentials or signing assets.

Android adapter: Gradle wrapper, default debug assemble and local JVM tests. Apple adapters: `xcodebuild`, shared schemes, unsigned development builds. iOS tests require a concrete available simulator destination selected by the project's test/CI configuration; generic build destinations do not mean tests ran. Apple test tasks are intentionally absent until real test targets exist. If Apple apps share a workspace/package, change registry argument arrays/required paths to the actual layout instead of keeping stale assumptions.

## Design workflow

Open `design/openmiraie/prototype/index.html` locally, or the journey map/gallery beside it. No packages or network are needed. The old `.stitch/flow/index.html` redirects locally for existing tabs. Browser automation may restrict local files; source/model checks do not substitute for visual walkthrough or native-device QA.

Curated assets and interaction specs belong under `design/openmiraie/`. Raw tool exchanges remain in ignored `.stitch/`. Historical exports may have old labels/paths; maintained prototype/spec take precedence.

## MCP server

The active `miraie-mcp` target owns its Python 3.11+ environment and locked dependencies. Install explicitly with `uv sync --frozen` inside `servers/miraie-mcp`, then run root `doctor`, `build` and `test` tasks for `miraie-mcp`. Build/test use offline dependency resolution after installation. The root doctor checks the local toolchain; `miraie-mcp doctor` checks local session storage, and the MCP `connection_test` is an explicitly requested read-only cloud check. See the [first-run guide](../servers/miraie-mcp/README.md). No build or test signs in or controls hardware.
