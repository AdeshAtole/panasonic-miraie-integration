# openMirAIe design

| Location | Purpose |
|---|---|
| [quiet-control/README.md](quiet-control/README.md) | Approved Quiet Control handoff: visual rules, interactions and acceptance |
| [quiet-control/quiet-app.html](quiet-control/quiet-app.html) | Approved connected full-app preview; synthetic data |
| [prototype/index.html](prototype/index.html) | Earlier clickable prototype; visual treatment and temperature Set are superseded |
| [prototype/map.html](prototype/map.html) | Product journey map |
| [flows/user-flows.md](flows/user-flows.md) | Complete action/state/recovery handoff; behavior adapts to native platforms |
| [design-system/DESIGN.md](design-system/DESIGN.md) | Historical initial tokens; Quiet Control supersedes the palette and controller |
| [prototype/gallery.html](prototype/gallery.html) | Earlier static Stitch screen studies, not a replacement for the current prototype |
| [explorations](explorations/) | Preserved Sleek and Stitch explorations with provenance; historical copy can be superseded |

This directory is product design source, not a native app or production web UI. Run prototype checks through `python3 scripts/project.py check`. No device, account or API is required. Browser visual verification and native accessibility/launcher testing remain separate from state-model checks.

The old `.stitch/flow/index.html` is a local compatibility redirect for already-open tabs; `.stitch/` itself is ignored tool working state. The approved preview lives in `quiet-control/`; the earlier prototype remains for historical and model-check reference. Past source snapshots inside `prototype/revisions/` are historical comparison material, not separately supported builds.
