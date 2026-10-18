# Room AC · full flow prototype

Open `index.html` in a browser. It is local and self-contained apart from neighboring CSS/JS; no external fonts, libraries, requests, secrets or API calls. Do not enter real credentials. Journey buttons reset to demo baselines; the next-request selector lets you try success, failure, unconfirmed, offline and expired-session behavior. Open `map.html` for an overview and `../flows/user-flows.md` for complete interaction and implementation requirements.

This is a purpose-built behavioral prototype derived from the approved Stitch visual direction, not a direct export of native app code. Supplementary new Stitch screens are in `../explorations/stitch-flows/`, with original and corrected assets distinguished. This prototype does not issue AC commands. Production apps live under apps/room-ac; the existing Android proof of concept is under prototypes/room-ac-android.

Browser tests/model checks and screenshots document prototype review only. Android system dialogs are simulated. Real controls/readbacks, native lifecycle, TalkBack, 200% text scaling, keyboard/IME behavior, exports and launcher resizing still need native verification.

## Visual refinement · 19 October

The clickable prototype has a refined visual system: restrained tonal colors, consistent local SVG icons, native-style switches, quieter secondary rows, revised type/spacing, cohesive sheets and widgets, and a quieter review frame. Earlier source files are preserved under `revisions/first-pass/` for comparison. Existing static Stitch boards are prior explorations; use the current clickable prototype as the visual target. Interaction logic remains unchanged and the state/render checks are rerun. Browser visual verification remains pending because the available browser tool blocks local-file navigation.
