# Philosophy

This is a meta repository for integrating with Panasonic MirAIe: reusable agent skills and protocol knowledge, alongside lightweight open implementations that people can use, customize and extend.

Its purpose is to make it easier to **understand, control, customize, record and observe the devices people already own**. The knowledge and the implementations should each be useful on their own, and make each other better.

## Reusable knowledge is a first-class product

Someone integrating MirAIe into a new app, agent, dashboard or workflow should be able to build on what has already been learned. Skills should explain how the integration works, what has been verified, where the boundaries are and how to add support responsibly.

Keep that knowledge portable and independent of any particular app, operating system or agent runtime. A contributor should not need to install our Android app or reproduce the original investigation to use the skill.

## Offer lightweight open implementations

The repository also provides practical implementations of that knowledge: native apps, an MCP server and focused supporting tools. These should be useful tools people can run themselves, inspect and adapt.

Keep their purposes clear and their parts independently usable. Installing an app should not require an unrelated server, and using the MCP server should not require a mobile app. Shared protocol understanding should allow different implementations without forcing one application framework on everyone.

## Less bloat, with deliberate tradeoffs

Keep the experience focused and the operating cost proportionate to the job. Dependencies, permissions, accounts, background services and infrastructure should have a clear reason to exist.

Evaluate the full cost: installation size, memory, battery, network traffic, maintenance and user complexity. A well-chosen dependency can reduce complexity; a bespoke implementation can add it. Prefer the simplest approach that remains reliable, secure and maintainable.

A lightweight product should still have a finished experience: straightforward setup, understandable controls and useful recovery when something goes wrong.

## Make customization a real choice

People should be able to decide how their devices fit into their lives. Support useful configuration, composable interfaces, accessible source and documented behavior so people can build their own controls, widgets, dashboards and automations.

Offer sensible defaults while keeping advanced possibilities discoverable. Preserve access to useful reported fields and integration details. Customization should respect capability boundaries and explicit user intent.

## Recording and observability belong in the design

Controlling a device is only part of understanding it. People should be able to inspect available readings, record observations over time and export their data for analysis or integration elsewhere.

Make recording behavior explicit: what is collected, when it is read, where it is stored, how long it is retained and how it can be exported. Keep collection user-controlled and proportionate to its purpose; continuous polling or a hosted data service should not be a prerequisite for basic use.

Observability also means making the integration understandable. Show timestamps, data freshness, connection state, command outcomes and useful diagnostics. Distinguish a requested setting from a reported setting, a fan setting from measured RPM, and a recorded observation from a newly measured sensor value. Missing or unverified data should remain identifiable.

These are design commitments. Each implementation must document which recording and observability features actually exist and which readings the selected device provides.

## Keep users in control of access and data

Use the user's authorized access transparently. Keep credentials in appropriate secure storage and private data out of logs, public examples and contributions. Avoid extra accounts, mandatory telemetry and unnecessary intermediaries.

Give users understandable choices about device changes, recording and external integrations. Setup and reconnection should not silently trigger appliance commands or replay old actions.

## Let evidence define support

MirAIe covers more devices than any one implementation has tested. Separate observed behavior from advertised capabilities, static findings and assumptions. Document support at the model and operation level.

Prefer clear limitations and honest failure states. A command being accepted does not prove the setting changed; a schema containing a sensor does not prove that a device reports it. Preserve those distinctions in skills, interfaces, tests and diagnostics.

## Contribute what we learn

New support, corrected protocol knowledge and reproducible bugs should improve the shared project. Contribute focused pull requests, useful tests, sanitized evidence and actionable issue reports so others benefit from each integration effort.

The repository succeeds when someone can integrate another device, build an alternative interface or understand their own device data with less duplicated work and more freedom of choice.
