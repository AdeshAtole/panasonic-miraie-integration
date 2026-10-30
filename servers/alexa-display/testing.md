# Alexa display verification

2026-10-30: offline synthetic tests pass via `python3 manage.py test`; ZIP builds
with `python3 manage.py build`. Tests exercise discovery, on/off routing, response
correlation, owner/client validation, no-op/state reads, strict model/capability
guards, online guards, display-only payloads, delayed verification, refresh storage,
no write retry, durable claims, replay across subsequent commands, changed-intent
collisions, crash-pending results and uncertainty blocking. No live account/device
requests are part of build or tests.

Still required before claiming end-to-end support:

- SAM/CloudFormation deployment and IAM/region/concurrency quota validation.
- Local Amazon callback and account linking in the owner's real browser.
- Dedicated cloud MirAIe login/expiry/refresh rotation; native apps remain usable.
- Alexa discovery, state queries, voice on/off, application switch UI and routines.
- Cold/warm end-to-end latency within Alexa's eight-second window.
- Physical LED observation and confirmation that AC power/settings remain unchanged.
- Manual recovery following an interrupted command; no replay after recovery.

Existing model-140013 protocol evidence belongs to the portable MirAIe skill.
Offline Alexa tests do not establish new model support or physical actuation.
