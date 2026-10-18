---
name: panasonic-miraie
description: Build Panasonic MirAIe India integrations, dashboards, widgets, automations, and telemetry collectors using reverse-engineered authentication, discovery, capabilities, REST controls, and MQTT evidence. Use for MirAIe appliances and partner devices; distinguish tested AC behavior from unverified device families. Separate Panasonic Comfort Cloud services require their own protocol.
---

# Panasonic MirAIe integration

Use this skill to integrate MirAIe into the user's chosen platform. It describes an unofficial protocol snapshot from MirAIe Android 01.4.32, investigated on 2026-10-18/19. It is not a universal device driver or a Panasonic API guarantee.

## Read the relevant reference

- Start with [platform-api.md](references/platform-api.md) for login, refresh, discovery, model lookup, status, and the verified control envelope.
- Read [capabilities.md](references/capabilities.md) before mapping entities, commands, units, or sensor readings.
- For an AC, use [air-conditioners.md](references/air-conditioners.md), including the verified example and UI/command dependencies.
- For other appliances, partner services, local control, or onboarding, use [device-coverage.md](references/device-coverage.md). Static category presence is a lead, not tested support.
- For push updates, use [realtime.md](references/realtime.md). MQTT transport details are established statically; topics, permissions, and full live delivery still need verification.
- For provenance and contribution requirements, use [evidence-and-extension.md](references/evidence-and-extension.md). The [capability registry](references/capability-registry.json), [route catalog](references/route-catalog.json) and [serialized schemas](references/serialized-schemas.json) are searchable static inventories, not ready-to-execute API recipes.

## Integration workflow

1. Discover all homes, spaces, and devices with the user's own account. Do not copy the prototype's AC-only category filter. Preserve hierarchy, model identifiers, parent/child relationships, and partner-service markers.
2. Fetch the selected model's capabilities and a status snapshot. Keep unknown capabilities and raw fields. Distinguish advertised commands, advertised sensors, and values actually observed.
3. Build the target platform's adapter around capabilities. Separate session management, discovery, model interpretation, command construction, reported state, and telemetry storage. Keep secrets in the target platform's credential store.
4. Route partner, IR, Matter, and local-protocol devices through their established transport. Do not assume the AC REST envelope, keys, or capability indexes apply universally.
5. For an authorized control test, record baseline state, choose a reversible change, send it once, and verify readback. A 2xx response means accepted, not physically applied. Wait for a dependent command to confirm before sending the next; bound polling and do not blindly resend timed-out mutations. Restore baseline settings after testing.
6. Expose reported state separately from requested state. Derive controls from actual choices and ranges, and show missing/offline/stale readings honestly. Offer raw-field inspection for unmapped telemetry.
7. Report support per model and operation as **live verified**, **static only**, **inferred**, or **unknown**. Include remaining work. Do not call the whole ecosystem supported because one AC works.

## What can be done without the appliance

Implement shared account/discovery plumbing, capability parsing, a generic read-only inspector, telemetry storage, adapters with fixture tests, and statically established command builders. A current authorized account response can establish discovery/read access without local physical access.

A physical device or a cooperating owner is needed to establish actual actuation, mode dependencies, sensor units/accuracy, firmware differences, availability behavior, and baseline restoration. Onboarding, LAN/BLE/IR/Matter flows need their relevant hardware and protocol evidence. Device ownership alone does not make a field name or untested command correct.

## Publication boundaries

Ship instructions, sanitized/synthetic fixtures, compact protocol facts, and provenance. Exclude account/device identifiers, credentials, MQTT topic values, pairing/UDP keys, private captures, signing material, APKs, and bulk decompiled source. This skill contains field names and public application identifiers, never account secrets. Installing or invoking it does not itself authorize changing appliances, pairing devices, or account administration.
