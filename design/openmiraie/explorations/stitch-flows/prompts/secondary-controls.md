Create a coherent mobile review board of Room AC's secondary control sheets, matching the existing controller and project design system. Show the actual sheets a person opens from Fan and More controls, with enough context to understand selection, confirmation and restrictions. Board annotations belong outside the product sheet faces.

**PLATFORM:** Mobile, native Android bottom sheets/dialogs. Provide readable distinct specimens for Fan selection, More controls, and capacity choice/confirmation; this is one design board, not unrelated dashboard pages. Respect system insets and at least48dp touch targets. Project design system supplies styling.

**PAGE STRUCTURE:**
1. **Fan sheet:** Title Fan speed, close/back affordance, single-choice Auto/Quiet/Low/Medium/High. Auto is the reported setting; a chosen High draft is visibly pending selection until Set fan speed is pressed. Cancel leaves actual Auto unchanged. Do not show measuredRPM. A short restriction specimen says Dry mode manages fan speed and disables manual choices; cleaning disables fan, too.
2. **More controls sheet in Cool:** EcoOff, PowerfulOff and CapacityNormal entries. Use clear labeled state/action affordances, not ambiguous checkmarks. Explain Eco sets26°C when enabled before the user commits. Powerful enabling turns off a fixed capacity. More controls require AC powerOn; Eco and Powerful are available in Cool only, and Powerful is unavailable during cleaning. Disabled reasons should be concise and visible when relevant.
3. **Capacity single-choice sheet:** Normal plus40%,50%,60%,70%,80%,90%,100%,110%; do not invent55%. Display Normal as the understandable name for no fixed capacity. Show80% selected as a draft with Set capacity and Cancel. A short confirmation states This turns Powerful off when it is currently on. It is a capacity setting, not an exact measured efficiency/energy-saving percentage.
4. **Eco confirmation specimen:** Turn Eco on? It also sets the target to26°C. Confirm and Cancel. Do not silently change the target before confirmation.
5. **Command feedback:** One concise state row illustrates Applying… while previous reported state is retained. A separate unconfirmed example uses Not confirmed yet with Refresh, not a success checkmark. Do not show pending and failure at the same time in one product sheet.

**BEHAVIOR CONTRACT:**
No real command sends from the design. In implementation, commands are serialized and read back. API accepted is not physical success. Powerful and fixed capacity are mutually exclusive; dependent changes confirm before the next command. Mode selection must be applied before its fan/temperature controls are available. No timers, schedules, swing, cleaning command, automation, heat mode or unsupported sensor claims are added. Keep the normal UI concise and readable; technical protocol notes go outside specimens.

Produce this one focused control-sheet board. Do not add new top-level screens beyond the specimens described.
