# openMirAIe design

| Location | Purpose |
|---|---|
| [prototype/index.html](prototype/index.html) | Current maintained clickable prototype; refined visual treatment and synthetic states |
| [prototype/map.html](prototype/map.html) | Product journey map |
| [flows/user-flows.md](flows/user-flows.md) | Complete action/state/recovery handoff; behavior adapts to native platforms |
| [design-system/DESIGN.md](design-system/DESIGN.md) | Approved initial tokens and direction; the current prototype contains later visual refinement |
| [prototype/gallery.html](prototype/gallery.html) | Earlier static Stitch screen studies, not a replacement for the current prototype |
| [explorations](explorations/) | Preserved Sleek and Stitch explorations with provenance; historical copy can be superseded |

This directory is product design source, not a native app or production web UI. Run prototype checks through `python3 scripts/project.py check`. No device, account or API is required. Browser visual verification and native accessibility/launcher testing remain separate from state-model checks.

The old `.stitch/flow/index.html` is a local compatibility redirect for already-open tabs; `.stitch/` itself is ignored tool working state. The maintained prototype has exactly one source here. Past source snapshots inside `prototype/revisions/` are historical comparison material, not separately supported builds.
