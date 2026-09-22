---
id: constraints
scope: GLOBAL
branch:
tags: [ARCHITECTURE, IMPL]
subjects: [use-case:get-context, use-case:update-context]
---
# Constraints

- Context is read only from the configured repository.
- Branch-scoped context must match the requested branch exactly.
- Tags classify documents; exact subject matches select them using OR matching.
- `GLOBAL` defines branch validity. Only `load: ALWAYS` enables automatic inclusion on a tool call.
- Relation expansion follows outgoing links one hop from subject matches and never crosses branch validity.
- Updates replace existing documents; they do not create or delete documents.
- An update must be rejected when its expected version is stale.
- HTTP access is local-only unless a real authentication boundary is added.
