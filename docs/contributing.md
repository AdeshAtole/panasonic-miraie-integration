# Contributing

Start with [architecture](architecture.md), [development](development.md), the target README and the [portable skill](../skills/panasonic-miraie/SKILL.md).

- Distinguish production app work from PoC investigation and design exploration. Keep changes in their owned directories.
- Run repository checks and relevant project tests. Report separately which native builds, visuals, accessibility checks and actual device behaviors were verified.
- Keep the skill installable by copying its directory alone. Retain evidence grades and exact model/operation scope; do not claim all MirAIe devices work because the test AC does.
- Publish sanitized/synthetic fixtures and concise evidence. Do not commit credentials, account/device identifiers, private captures, APKs, signing files, bulk decompiled source or raw generation exchanges.
- Build/test does not authorize changing an appliance. Live verification needs the user's authorization and bounded readback; restore baseline where agreed.
- Add dependencies/build tools only within the project that needs them. Shared code requires real consumers and tests. Activate platform targets only when actual native projects exist.
- Avoid global SDK/configuration changes as a prerequisite for repository scripts; prefer environment variables and project-owned pins.

No push, release, app installation, store distribution, signing-account setup or live AC control is part of ordinary repository validation.
