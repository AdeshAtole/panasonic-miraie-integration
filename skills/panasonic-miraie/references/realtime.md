# MQTT and other transports

## Established statically

The inspected `MQTTService` uses a broker string `ssl://mqtt.miraie.in:8883`. The `ssl://` notation belongs to its Java MQTT library; configure TLS appropriately for the target library rather than assuming every client accepts that URL scheme.

- Username: homeId, or `homeId@appBrand` when appBrand is nonempty.
- Password: the account access token.
- Clean session: enabled in the inspected connect method.
- Library automatic reconnect: disabled there; the service has its own reconnect path. This is not a requirement to disable reconnect in a new integration.
- Keepalive: configured when the service's supplied interval is positive; no universal numeric default was established.
- Publish helper: serializes supplied JSON and publishes to a **caller-supplied topic**, with caller-supplied QoS and retain flag.

The serialized discovery model includes home-level `metaTopic` and device-level `topic` lists. Use account-returned topic metadata as investigation input; matching the correct subscription/control topic roles still needs caller evidence. Generic BaseDevice code also distinguishes status, pstatus, control and firmware topics.

## Still unknown / not live-tested here

The complete account-specific topic construction, subscription authorization, effective QoS/retention, client-ID policy, broker token-rotation behavior, message order, delivery completeness and live push flow were not established. Do not synthesize a topic from homeId/deviceId or publish the REST body to an assumed MQTT topic. The generic publish helper does not establish the caller's payload contract.

To add MQTT, trace the original service setup and subscribe/publish callers or inspect an authorized runtime/capture. First verify a read-only subscription on the exact account-derived topics. Treat topic values as private. Record envelope/schema, timestamps, retain/QoS behavior, duplicate/out-of-order handling and reconnect/resubscribe behavior. Verify a status baseline via REST where supported; do not infer online state solely from an open broker connection. Never retain actuator commands without verified native semantics.

## LAN, BLE, Matter, IR and partner services

The APK declares Matter commissioning/NOC routes, device registration, UDP control key/IV fields, IR/virtual remotes, video services and partner account paths. Their existence does not establish a functioning local integration or a shared encryption scheme. Do not derive local keys or bypass cloud authorization from field names. Inspect each transport's actual builders, framing, identity and lifecycle separately, with relevant hardware or a cooperating owner. Comfort Cloud and Anchor auth routes in the same APK are not interchangeable with the tested MirAIe login.
