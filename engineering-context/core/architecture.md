---
id: architecture
scope: GLOBAL
branch:
tags: [ARCHITECTURE, IMPL]
---
# Architecture

MCP is the control plane. It exposes two tools: `get_context` and `update_context`.

The coding assistant turns its task into a structured request containing the exact Git branch and relevant tags. The context service returns complete matching Markdown documents. Global documents and documents belonging to the exact branch are eligible; requested tags use OR matching.

Updates create documents for new stable IDs and fully replace existing documents. New files use `engineering-context/<id>.md`; existing documents retain their paths and must be visible on the requested branch. The service validates all documents before writing a batch. Storage remains behind the `IContextDao` interface.
