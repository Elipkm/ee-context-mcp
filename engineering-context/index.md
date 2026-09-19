---
id: context-catalog
scope: GLOBAL
branch:
tags: [DOCUMENTATION]
subjects: [domain:context-engine]
---
# Engineering context map

Each Markdown document below `engineering-context/` has a stable ID, branch validity, classification tags and optional subjects, relations and load policy. The engine selects by subjects and connections; this page is a human-readable catalog. Available subjects include `domain:context-engine`, `component:context-service`, `component:context-storage`, `component:mcp-server`, `component:context-engine`, `use-case:get-context` and `use-case:update-context`.

## Global project context

- [Domain](core/domain.md) — Product vocabulary, developer workflow, task context and update proposals.
- [Business goals](core/business-goals.md) — Demo purpose, success criteria and audience value.
- [Architecture](core/architecture.md) — Two-tool MCP contract, filtering, versions and DAO integration.
- [Tech stack](core/tech-stack.md) — Java, Spring Boot, Spring AI MCP, Maven and Markdown choices.
- [Constraints](core/constraints.md) — Branch validity, subject matching and replacement rules.

## Engineering guidelines

- [Implementation](guidelines/implementation.md) — Coding style, component responsibilities and error handling.
- [Documentation](guidelines/documentation.md) — Markdown structure, context freshness and change documentation.
- [Testing](guidelines/testing.md) — Unit and integration testing expectations for selector, scope, updates and MCP.

## Branch-specific work

- [Context engine implementation](work/active/context-engine/overview.md) — Current MCP endpoint behavior, context-engine decisions and acceptance criteria.
- [Context engine progress](work/active/context-engine/progress.md) — Implemented work, remaining work and verification notes.
