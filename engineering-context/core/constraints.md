---
id: constraints
scope: GLOBAL
branch:
tags: [ARCHITECTURE, IMPL]
---
# Constraints

- Context is read only from the configured repository.
- Branch-scoped context must match the requested branch exactly.
- Tags use OR matching.
- Updates replace existing documents; they do not create or delete documents.
- An update must be rejected when its expected version is stale.
- HTTP access is local-only unless a real authentication boundary is added.
