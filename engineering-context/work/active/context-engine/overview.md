---
id: context-engine-overview
scope: BRANCH
branch: codex-impl-2
tags: [ARCHITECTURE, FEATURE, IMPL]
---
# Context engine implementation

## Acceptance criteria

- MCP exposes only `get_context` and `update_context`.
- Inputs are structured DTOs assembled by the coding assistant.
- Retrieval returns complete documents with IDs, versions and tags.
- Global and exact-branch documents are filtered with OR tag matching.
- Updates create new documents by ID or fully replace existing documents visible on the requested branch.
- Local Markdown access is replaceable through `ContextDao`.

## Current decisions

Selection uses scope and tags only. The coding assistant owns the semantic decision about which tags its task requires.
