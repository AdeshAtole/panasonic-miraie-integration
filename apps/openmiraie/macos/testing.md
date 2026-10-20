# macOS verification

Run `python3 scripts/project.py test macos` from the repository root or `python3 manage.py test` here. Tests run native XCTest on this Mac, using a separate ignored `build/Tests` directory. No simulator is required.

The scheme sets `OPENMIRAIE_NO_NETWORK=1`, the app delegate skips session restoration/status-item creation, and the real cloud transport refuses all network requests. Tests inject synthetic transports and memory sessions; one Keychain test uses an isolated random service and deletes it afterward. Test builds disable the app sandbox to export synthetic native review images; the normal app build retains its sandbox and network-client entitlement.

## Automated scope

- Native Keychain save/update/load/delete; tokens only, separate app identity.
- MirAIe email/mobile payloads, encoded identifiers, read refresh/rotated tokens and no mutation replay on authorization failure or timeout.
- Discovery hierarchy, category/routing/model guards, capability-derived ranges/choices/indexes, temperature units, independent display/power, Dry/Fan/cleaning/sleep/JetStream restrictions and integer swing positions.
- Fresh preflight, bounded readback, sequential coupled actions, partial outcomes, session changes and already-enabled Eco with an unmet target effect.
- Release-only temperature submission and coalesced increments; dismissal cancels drafts without a write. Unconfirmed/stale reports block changes. Room switches cannot show the previous room’s report. Sign-out clears the UI without appliance writes.
- Menu label omits pending, unconfirmed, offline, stale and unavailable targets.
- Real native SwiftUI rendering in light/dark for ready, sign-in, offline, Off, Fan, Dry, cleaning, pending and unconfirmed states, attached to the XCTest result and exported to a temporary `openmiraie-macos-review` folder. These are synthetic view renders, not live-device or accessibility certification.

## Remaining manual acceptance

1. Open the built app: confirm the menu bar icon/popover, click-away/Escape dismissal, Quit and no Dock icon. Walk through native sign-in and explicit device selection. Test long room names, multiple displays and system appearance changes.
2. With VoiceOver and keyboard navigation, inspect labels, temperature adjustments, menus, disabled-state explanations, focus restoration and enlarged text. Snapshot rendering alone does not establish accessibility.
3. With separate explicit authorization, verify a reversible live setting on the intended AC, its cloud readback and physical behavior, then restore baseline. Test offline, token expiry/reconnection, cancellation on sleep, failed Keychain access and coupled partial completion. Build/test authorization does not authorize these writes.
4. Configure distribution signing/notarization separately. Local ad hoc signing is a development artifact; launch-at-login and automatic updating are not implemented.
