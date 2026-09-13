---
id: architecture
scope: GLOBAL
branch:
tags: [ARCHITECTURE, IMPL]
---
# Architecture

MCP is the control plane. It exposes two tools: `get_context` and `update_context`.

The coding assistant turns its task into a structured request containing the exact Git branch and relevant tags. The context service returns complete matching Markdown documents. Global documents and documents belonging to the exact branch are eligible; requested tags use OR matching.

Updates are complete document replacements addressed by stable document ID. A version from `get_context` must match before an update is written. Storage remains behind the `ContextDao` interface.
