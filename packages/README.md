# Shared libraries

No runtime library has been extracted yet. This directory is a boundary for reusable production code once there are actual consumers, not a requirement to share a language/UI runtime across platforms.

The portable MirAIe skill remains the protocol/evidence reference. First share sanitized fixtures and command/readback expectations; keep one canonical copy of each contract and reference it from tests. A future Swift package can share account/discovery/capability/session logic between native iOS/macOS clients. Android may have its own native module(s).

Do not import the PoC as a production SDK, add placeholder packages with fake tests, move account secrets into fixtures, or extract generic abstractions before there is a concrete consumer. Each package must own its tests, dependencies, build metadata and a README naming consumers when introduced.
