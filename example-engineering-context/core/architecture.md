---
id: architecture
scope: GLOBAL
branch:
tags: [ARCHITECTURE, IMPL]
load: ALWAYS
subjects: [component:context-service, use-case:get-context]
---
# Architecture

MCP is the control plane. It exposes two tools: `get_context` and `update_context`.

For feature or bug work needing broader system context, the coding assistant supplies the exact Git branch and at least one subject. The context service returns complete documents valid on that branch: short `load: ALWAYS` baselines, any exact subject matches, and outgoing relation targets one hop from those matches. Baseline-only documents do not trigger expansion. Results are deduplicated; tags classify documents without filtering retrieval. `GLOBAL` means valid on all branches, not always loaded.

Updates create documents for new stable IDs and fully replace existing documents. New files use `engineering-context/<id>.md`; existing documents retain their paths and must be visible on the requested branch. The service validates all documents before writing a batch. Storage remains behind the `IContextDao` interface.
