---
id: domain
scope: GLOBAL
branch:
tags: [BUSINESS_RULE, DOMAIN]
---
# Domain

The engine supports a developer working with a coding assistant on one repository task.

- A **context document** is Markdown with a stable ID, scope, branch and tags.
- A **global document** is eligible on every branch but is returned only when its tags match.
- A **branch document** is eligible only when its branch matches the request exactly.
- A **version** identifies the complete stored document and protects updates from stale writes.

The coding assistant decides which tags a task needs and how to use the returned documents. The engine owns deterministic filtering and safe full-document replacement.
