---
id: domain
scope: GLOBAL
branch:
tags: [BUSINESS_RULE, DOMAIN]
subjects: [domain:context-engine]
---
# Domain

The engine supports a developer working with a coding assistant on one repository task.

- A **context document** is Markdown with a stable ID, branch validity, classification tags, subjects, relations and load policy.
- A **global document** is valid on every branch, but not automatically loaded.
- A **subject** identifies a domain, component or use case using an exact `kind:name` identifier.
- A **relation** connects a document to another document by ID and records the semantic connection type.
- An **ALWAYS document** provides short baseline context whenever retrieval is called on a valid branch.
- A **branch document** is eligible only when its branch matches the request exactly.
- A **version** identifies the complete stored document and protects updates from stale writes.

The coding assistant chooses subjects for feature or bug tasks needing broader context. The engine selects subject matches and baselines, expands outgoing relations from subject matches for one hop, and supports full-document replacement.
