Create one mobile design board showing two practical Android launcher widget proposals for Room AC, matching the controller screen and the project's existing design system. They control the currently selected synthetic Bedroom AC. The board is a review artifact; annotations are outside the actual widget faces.

**PLATFORM:** Mobile, Android home-screen widget design, portrait review board. Show2×2 and4×2 target cell sizes; launcher dimensions vary, so preserve usable target areas rather than pretending cell labels have universal pixel dimensions. No full app navigation is embedded in the widget.

**PAGE STRUCTURE:**
1. **Board heading:** Room AC widgets, with concise Design proposals label outside the widget faces.
2. **2×2 compact widget:** Bedroom header opens the app. Read-only target23°C, Cool mode, explicit current powerOn and updated10:24. Two bottom actions: a visible Off power action and Refresh. Actions have accessible names and at least48dp targets. Use a compact layout with sufficient space for the target, state and buttons; do not overlap elements to force an unrealistically small box. Tapping the target opens the controller; no temperature+/− command at this size.
3. **4×2 wide widget:** Bedroom header, target23°C, smaller Room24.18°C, current On · Cool state and updated10:24. Explicit Off, Display and Refresh actions. Display shows its actual On state and is independent of AC power. Keep the read timestamp visible. A small board annotation outside the widget says Display action proposed, because the shipped widget currently has On/Off/Refresh only.
4. **Short implementation note outside both widgets:** Manual refresh; last-known values remain while pending; unknown readings appear unavailable. Both layouts are proposed, not an already installed update.

**STATE AND BEHAVIOR:**
- Show one coherent confirmed sample state in each widget. Do not show pending and error together. Document alternate pending/error behavior through a short board note, not overlapping notification text inside the widget.
- A refresh does not imply continuous polling. Commands verify reported-state readback; accepted is not physical confirmation. A pending label can temporarily replace Updated10:24, preserving last-known readings. A failed/unconfirmed state offers Refresh rather than falsely changing state.
- Fan/temperature/mode controls belong in the app. No schedules, energy graph, liveRPM, humidity or automation. No account/device identifiers, only synthetic Bedroom and sample readings.

Produce this one widget board only. Keep annotations concise and separate from the actual launcher widget specimens.
