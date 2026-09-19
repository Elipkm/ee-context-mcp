---
id: context-engine-overview
scope: BRANCH
branch: codex-impl-2
tags: [ARCHITECTURE, FEATURE, IMPL]
load: ALWAYS
subjects: [component:context-engine]
relations:
  - type: REQUIRES
    target: architecture
---
# Context engine implementation

## Acceptance criteria

- MCP exposes only `get_context` and `update_context`.
- Inputs are structured DTOs assembled by the coding assistant.
- Retrieval returns complete documents with IDs, versions and tags.
- Global and exact-branch documents are selected by exact subjects, `load: ALWAYS` and one-hop outgoing relations. Tags only classify documents.
- Updates create new documents by ID or fully replace existing documents visible on the requested branch.
- Local Markdown access is replaceable through `ContextDao`.

## Current decisions

Selection uses branch validity, baseline load policy, subjects and explicit outgoing relations. The coding assistant chooses subjects and calls retrieval only for feature or bug work needing broader context. Relations from baseline-only documents are not expanded.
