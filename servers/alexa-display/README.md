# Alexa AC display switch

Private, single-owner Alexa Smart Home v3 skill for the MirAIe India AC display.
It discovers one **switch named “AC display”**. Say:

- “Alexa, turn off AC display.”
- “Alexa, turn on AC display.”

The switch's `powerState` represents the **display light**, not the AC's power.
Only `acdc` is written, using `DisplayControl`, registry index 8. Keep Panasonic's
existing Alexa skill for power, temperature and mode. No changes to that skill
or its account link are necessary. A custom third-party skill cannot extend
Panasonic's existing endpoint; it supplies this additional endpoint.

## Status and scope

Implemented and tested offline; **not deployed, certified or tested through an Echo**.
Display protocol evidence comes from the project's reviewed model **140013**
(CS-CU-NU18BKY5WX), not from new Alexa actuation tests. Every discovery/report/control
fetches current model capabilities and online status. Unknown models, partner/child
devices and altered display keys fail closed. See the portable
[protocol](../../skills/panasonic-miraie/references/platform-api.md),
[AC mappings](../../skills/panasonic-miraie/references/air-conditioners.md) and
[capability guidance](../../skills/panasonic-miraie/references/capabilities.md).

This first version intentionally fills the missing display control. Extra fan,
Eco, capacity or airflow entities can be added after choosing appropriate Alexa
interfaces and preserving the reviewed model-specific preconditions/coupled effects.
This is not a public multi-customer service: it binds exactly one Amazon owner to
one explicitly selected AC. Public distribution requires per-customer session/device
storage, onboarding, privacy policy and Amazon certification.

## Files and offline commands

- `src/skill.py`: discovery, directives, LWA owner validation and DynamoDB journal.
- `src/protocol.py`: small independent adapter using Python's TLS client.
- `template.yaml`: AWS SAM infrastructure; one Lambda, one journal table, restricted invocation.
- `setup.py` / `owner_login.py`: explicit, interactive sign-in, device selection and recovery.
- `tests/`: synthetic tests; no credentials, account requests or hardware writes.

Python 3.11+ is sufficient to build/test. Lambda targets Python 3.12 and uses its
AWS-managed boto3/botocore. There are no other runtime packages. The AWS SDK is
runtime-managed rather than pinned by this repository; deployment validation must
exercise that runtime. Run from the repository root:

```sh
python3 scripts/project.py test alexa-display
python3 scripts/project.py build alexa-display
```

Build creates `build/display-skill.zip`. It neither logs in nor deploys.
Setup requires boto3 installed explicitly in a dedicated local Python environment;
deployment requires AWS CLI and AWS SAM CLI. AWS resources may incur charges.

## Set up your own private skill

Use the same Amazon account for the Alexa developer console and Alexa app.
Create a Smart Home skill (Amazon's current documentation also calls it a smart
home add-on), v3, with English (India), and keep it in development/testing.
Record its skill ID. No custom invocation phrase or interaction model is required.

1. Create a **Login with Amazon security profile** in the Amazon developer console.
   Under Web Settings add `http://localhost:8765/callback` as an Allowed Return URL
   for the one-time local owner sign-in. Keep the client ID and client secret private.
2. In the skill's Account Linking page enable **authorization code grant**:
   Authorization URI `https://www.amazon.com/ap/oa`, Access Token URI
   `https://api.amazon.com/auth/o2/token`, your LWA client ID/secret, scope
   `profile:user_id`, HTTP Basic client authentication. Copy **every Alexa redirect
   URL shown by the console** into the same LWA profile's Allowed Return URLs.
   Do not enable proactive-event permissions or app-to-app linking for this version.
3. Configure AWS authentication locally with the normal AWS CLI sign-in flow.
   Use **eu-west-1 (Ireland)** for Europe/India. Review which AWS account is selected.
   Run the following manually from this directory after installing boto3 locally:

   ```sh
   python3 setup.py --region eu-west-1 --secret openmiraie/alexa-display connect
   ```

   It prompts for MirAIe credentials with hidden input, reads devices, requires an
   explicit selection, then opens Amazon sign-in in your browser. The local callback
   validates a random OAuth state and Host header. The LWA client secret is used once
   to exchange the authorization code and is not saved. A **dedicated** MirAIe session,
   selected device and authorized Amazon owner are saved directly to AWS Secrets
   Manager. No existing local MCP session is read/exported. No password or LWA token
   is stored. Do not paste any credentials or tokens into chat.
4. Copy the secret's ARN privately from AWS Secrets Manager. Review `template.yaml`,
   then build and deploy with SAM:

   ```sh
   sam build --template-file template.yaml
   sam deploy --guided --region eu-west-1
   ```

   Enter your skill ID and dedicated configuration secret ARN as parameters. The
   template grants the Lambda only access to that secret and its own journal table.
   It restricts invocation to Alexa Smart Home and this skill ID. It creates no
   public HTTP endpoint. If using a customer-managed KMS key, add narrowly scoped
   decrypt/encrypt permissions yourself; the template assumes the default service key.
5. Set the skill's Default/Europe-and-India endpoint to the deployed **FunctionArn**.
   Enable testing and enable/link your development skill in the Alexa app using the
   same owner signed in during setup. Run discovery; expect one “AC display” switch.
   Keep it out of broad “all switches” routines unless those should affect the display.
6. When ready to operate the AC display, say “Alexa, turn off AC display.” Verify the
   physical LED and confirm the AC continues running, then try display on. These are
   manual live acceptance steps; build/setup/discovery never perform them.

The Lambda's reserved concurrency is **one** so refresh and commands cannot race.
Some new AWS accounts cannot reserve concurrency until their regional quota is
increased. Resolve that quota if deployment rejects the setting; do not remove
serialization. Cold starts/cloud latency can exceed Alexa's eight-second response
window. This version uses short HTTP budgets, one POST and one delayed readback;
it has no background queue or deferred-event delivery. Actual regional latency and
refresh rotation remain deployment acceptance checks.

## Failure and recovery

Each control message ID is hashed and claimed persistently before processing.
Replayed IDs return their saved result, including after later commands or cold
starts. A reused ID with a different target is rejected. Before POST, a durable
uncertainty record is written. A timeout, process termination, rejected/ambiguous
POST or unconfirmed readback leaves that record in place and blocks new writes.
Never automatically retry a failed voice command. Reports remain read-only.

Readback occurs once, at least three seconds after acceptance. Success means the
cloud reported the requested display state; it does not prove the physical LED
changed. Already-matching requests send no POST. No power, mode or temperature
command is sent as a side effect.

To inspect a blocked outcome, wait at least 30 seconds after the command, then run
locally with the JournalTable output:

```sh
python3 setup.py --region eu-west-1 --secret openmiraie/alexa-display reconcile --table YOUR_TABLE
```

Review the reported result and the AC. Add `--clear-uncertain` only when you intend
to acknowledge that outcome and allow **new** voice requests. This sends no device
command, never replays an old request and preserves old request deduplication.
Pause/disable the skill before reconnecting or administering its cloud session:

```sh
python3 setup.py --region eu-west-1 --secret openmiraie/alexa-display reconnect
```

Reconnect preserves the owner/device binding and verifies device ownership.
The journal has no TTL: retaining message hashes prevents delayed replay; it grows
with explicit requests. It stores safe Alexa responses/correlation IDs, targets and
uncertainty timestamps, not account tokens or sensor history. There is no polling
or analytics. Lambda intentionally logs no raw events, URLs or backend errors.
Use CloudWatch invocation/error metrics for diagnosis without enabling event logs.
To retire the integration, disable the skill, delete the stack, then explicitly
delete the retained journal and dedicated secret when no longer needed. Deleting
the journal resets replay protection; do not reuse it with an active old skill.

## Alternatives

- **Home Assistant → Alexa**: useful if you already run Home Assistant. A MirAIe
  adapter/template switch can represent the display and be exposed through its
  Alexa integration. Verify that your chosen MirAIe adapter supports the display
  and handles session/command semantics. It adds an always-on Home Assistant host.
- **Custom voice skill**: can use a phrase such as “Alexa, ask AC controls to hide
  the display.” It avoids a separate discovered switch but needs an invocation name
  and still needs a backend and secure account mapping.
- **MirAIe scene exposed to Alexa**: potentially less work if the official app lets
  you create a display-only scene and Alexa actually discovers it. That support has
  not been established here; an Alexa routine alone cannot add a missing command.
- **ToggleController on an AC endpoint**: a semantic option if we later replace the
  full climate integration. A separate PowerController switch is simpler for the
  requested short phrase and coexists with Panasonic's current endpoint.

Sources: [PowerController](https://developer.amazon.com/docs/alexaplus/device-apis/alexa-powercontroller),
[Amazon account-linking tutorial](https://developer.amazon.com/ja/docs/alexaplus/smarthome/set-up-account-linking-tutorial.html),
[LWA token validation](https://developer.amazon.com/docs/login-with-amazon/obtain-customer-profile.html),
[local URL support](https://developer.amazon.com/docs/login-with-amazon/faq.html),
[Alexa timing](https://developer.amazon.com/en-US/blogs/alexa/device-makers/2020/12/common-issues-in-alexa-smart-home-skills-and-how-to-troubleshoot-them),
[Home Assistant Alexa](https://www.home-assistant.io/integrations/alexa.smart_home/).
