# Evidence and extending coverage

## Artifact identity

- Android package: `com.panasonic.in.miraie`
- Version: `01.4.32` / code 231301432
- Base APK SHA256: `62a36f534b6e38802e9a1ff2138a6342d1328e0f0a4928ce37ba2299e2f867a1`
- Static toolchain: REA with JADX headless MCP 0.7.1; engine SHA256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`
- Investigation date: 2026-10-18/19. Only base APK was inspected; splits, all native libraries and every runtime branch were not exhaustively analyzed.

This public folder carries compact facts and extracted annotation/schema metadata. It deliberately excludes the APK, bulk decompiled source and private runtime captures. Retained REA IDs in `evidence-index.json` identify the original investigation's records; they are provenance identifiers, not publicly resolvable URLs or a guarantee those records exist in another agent's session. The facts needed to use this skill are included here.

`route-catalog.json` contains 209 Retrofit-annotation records from the artifact. It preserves declaring interface, method, DEX parameter descriptors, return type and method annotations. It does **not** contain all parameter-level Path/Query/Body annotations, host/interceptor bindings, or every dynamic URL. Method-level declaration presence does not prove an endpoint is still deployed or supported on an account. The tested control path, for example, was established through separate builder/runtime evidence.

`serialized-schemas.json` contains Gson-annotated field names and DEX types. It is a field inventory, not a complete runtime JSON schema: generics, nullability, enum constraints, inherited/unannotated properties, serializers, validators and defaults may be missing. `Z`/`I`/`F`/`J` mean Java boolean/int/float/long; `L...;` denotes an object; `[` an array. No field values from a user's account are included.

## Claim labels

| Label | Required basis |
|---|---|
| Live verified | Exact operation on a named model, response/readback and date; physical effects identified separately |
| Static only | APK caller/builder/annotation, artifact identity and source/evidence ID |
| Inferred | Reasoning from facts, stated explicitly; not promoted to support |
| Unknown | Missing key, unit, transport, account access, acknowledgement or hardware behavior |

A bounded search with no RPM field does not prove the server/firmware cannot supply RPM. A fixture test verifies parsing, not hardware. A command response/readback establishes cloud-reported state; where physical behavior matters, corroborate with the owner or observation.

## Extending to another model or family

1. Obtain authorized discovery, exact model metadata, and a raw status sample. Record model, firmware if reported, app version, region, transport, and observation date. Sanitize before committing.
2. Locate the capability builder and caller in the matching APK. Recover wire key from the model, value type/enums/ranges, registry index if used, and required companion fields. Trace UI guards and dependent feature resets. Do not brute-force indexes or enum values against appliances.
3. Establish which transport owns the device, including partner tokens, parent/child identities and separate host bindings. A static category name does not justify using AC REST controls.
4. Add offline fixtures for representative, missing, malformed, offline and out-of-range data. Test the adapter's observable behavior, not copied documentation wording.
5. With the owner/device available, test a reversible action and readback, one change at a time. For heaters, washers, locks, cameras and similar devices, choose an appropriate observable test and preserve real operating constraints. Do not start a physical cycle just to see if a guessed command works.
6. Restore baseline, document what actually passed, and retain explicit unknowns. Add a device-specific reference only once useful semantics are established. Avoid marking an entire family supported based on one model.

For reverse engineering, REA is an optional investigation tool, not a runtime dependency of integrations or this skill. Use matching local artifacts and retained evidence when available; do not silently install global tooling/configuration or fetch a different version and attribute its behavior to this snapshot.

## Publication hygiene

Commit this skill folder and independently reviewed source/tests. Do not publish account tokens, real device/home/user IDs, MQTT topic values, serial/MAC/network details, location/contact data, pairing/registration/UDP secrets, personal sensor histories, UI dumps, developer signing keys, downloaded proprietary binaries or unreviewed bulk captures. Fields named `udpControlKey`, `manualCode` or `deviceRegistrationTokenEncrypted` are sensitive values even when nested or encrypted. A denylist alone is not enough; build public fixtures from an explicit allowlist or synthetic values. The public clientId above is an application identifier, not an account credential.
