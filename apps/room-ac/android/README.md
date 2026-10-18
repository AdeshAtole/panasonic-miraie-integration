# Room AC · native Android

Stage: **design**, not yet buildable. This is the production app location; the working integration PoC is under `prototypes/room-ac-android/`.

When implementation starts:

1. Scaffold a native Android Gradle project here with an `app` module, committed Gradle wrapper (including its JAR), pinned plugin/dependency versions and Java/toolchain requirements. Select native UI technology explicitly during implementation; the PoC's ad-hoc Java build does not dictate it.
2. Keep widgets, resources, platform tests and native secure storage inside this project. Implement the shared user-flow behavior using the protocol skill, not imports from the PoC.
3. Supply `assembleDebug`, meaningful local unit tests and an instrumented/device verification plan. Commit locks/verification metadata appropriate to the chosen Gradle configuration.
4. Change `android` to `active` in `projects.json`, confirm commands/required files match the actual project, and add a real Android CI build/test job. Keep release signing separate from local development and CI smoke builds.

Root commands are reserved as `python3 scripts/project.py build android` and `test android`. They deliberately fail with this stage explanation until the production project is activated. No empty app or fabricated successful build is provided.
