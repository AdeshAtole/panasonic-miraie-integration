# Shared platform API

## Provenance and scope

India MirAIe package `com.panasonic.in.miraie`, version `01.4.32`, version code `231301432`. Login, discovery, model-ID lookup, mobile status, AC REST commands and Daily energy history worked on one account/device. Refresh was implemented from shipped definitions but expiry/refresh was not exercised. Hosts and app identifiers can change; treat these as a reproducible snapshot.

## Authentication

| Purpose | Observed value |
|---|---|
| Authentication base | `https://auth.miraie.in/simplifi/v1/` |
| Main application base | `https://app.miraie.in/simplifi/v1/` |
| Public Android application clientId | `292d99d7-5623-4157-8a71-846ee3ff5dbf` |

POST authentication-base `userManagement/login`, JSON:

```json
{"email":"<user-supplied-email>","password":"<user-supplied-password>","clientId":"292d99d7-5623-4157-8a71-846ee3ff5dbf","scope":"an_<unique-session-suffix>"}
```

For phone login, replace `email` with `mobile`; preserve the user's working account format rather than guessing normalization. The working prototype generated the suffix from a monotonic counter; exact server scope requirements are not fully characterized. Response schema has `accessToken`, `refreshToken`, `expiresIn`, `userId`; treat `expiresIn` as the schema's seconds-valid field, without assuming an actual lifetime. Use `Authorization: Bearer <accessToken>` on main-base calls. Send JSON `Accept` and `Content-Type` headers.

POST authentication-base `userManagement/tokenRefresh`:

```json
{"clientId":"292d99d7-5623-4157-8a71-846ee3ff5dbf","token":"<refreshToken>"}
```

The prototype attempts one refresh on HTTP 401 or 406, stores a rotated refresh token if supplied, then retries the original request once. The 406 policy is a prototype behavior, not proof every 406 means expiration. Serialize refresh across concurrent clients. Do not loop on invalid credentials or replay non-idempotent commands after an ambiguous network failure. Distinguish authorization failure from device offline or unsupported operations. Use normal TLS certificate validation; do not forward credentials across unverified redirects.

Keep credentials out of prompts, shell history, URLs, logs, and fixtures. The reference Android implementation encrypted session tokens using Keystore AES-GCM and did not save passwords. Other platforms should use their own credential facilities; do not make Android a dependency.

## Discovery and read access

Paths below are relative to the main base unless otherwise specified.

| Method/path | Evidence | Use |
|---|---|---|
| GET `homeManagement/homes` | Live | Traverse each home → `spaces[]` → `devices[]` |
| GET `deviceManagement/deviceModels/modelId/{modelId}` | Live | Selected device's model capabilities |
| GET `deviceManagement/deviceModels/modelNumber/{modelNumber}` | Static | Alternate model lookup; verify parameter and response on target |
| GET `deviceManagement/devices/{deviceIdentifier}/mobile/status` | Live using deviceId in placeholder | Current cloud-reported status |
| GET `deviceservices/device/{deviceId}/config` | Static | Configuration route, not necessarily a universal readable capability map |
| GET `powerConsumption/devices/{deviceId}` | Daily live; other grains static | Energy history when supported |

URI-encode individual identifiers. Preserve original IDs internally, not in exported examples. The working discovery response was a homes array; do not assume every endpoint has the same wrapper. Inspect the actual response before adapting `data`, `status`, `params`, or `state` wrappers. Blindly unwrapping arbitrary fields can discard real data.

Record device category, modelId/modelNumber, parentDeviceId, thirdPartyService/model markers, and hierarchy. Do not infer category codes from friendly names or use the test AC model ID globally. A discovery entry is not evidence that every generic endpoint applies to it.

## REST control: verified AC path

POST `deviceManagement/control`:

```json
{"deviceId":"<selected-device-id>","control_params":{"cnt":"an","sid":"0","ps":"on","ki":1}}
```

`cnt:"an"` and `sid:"0"` were used by the working integration. Their full semantics are unresolved. The command key must come from the selected model's capability attributes. `ki` is a native capability registry index; it is neither an array position nor the modelId. Registry collisions and aliases exist, so it is not a globally unique capability identifier; bind the implementing capability class, actual model and builder together. Read [AC mappings](air-conditioners.md) for verified indexes. Do not invent indexes for another capability or device family.

Read the selected device status after sending. Compare string enums case-insensitively where the protocol allows, and numeric strings numerically. Preserve raw reported values. The tested app used up to five increasing waits of 0.6/1.2/1.8/2.4/3 seconds between status reads and stopped dependent commands if prior confirmation failed. This is a bounded implementation choice, not a published rate-limit contract. Unknown service quotas require conservative polling and backoff on server errors.

## Energy history

Query parameters: `grain`, `startDate`, `endDate`. Preserve exact case.

| Grain | Date format | Verification |
|---|---|---|
| `Daily` | `ddMMyyyy` | Seven entries returned live |
| `Weekly` | `ddMMyyyy` | Shipped caller only |
| `Monthly` | `MMyyyy` | Shipped caller only |

Returned model fields include `day`, `week`, `month`, `power`, represented as strings. Daily values are displayed by the original app as **kWh**, with no watts-to-kWh conversion in the inspected display path. This establishes app interpretation, not calibrated meter accuracy. Current-day totals can be partial. Daily exports used ISO dates and retained decimal precision. Do not label energy as instantaneous watts or assume every appliance supports this route.
