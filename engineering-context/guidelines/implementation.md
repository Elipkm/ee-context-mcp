---
id: implementation-guidelines
scope: GLOBAL
branch:
tags: [IMPL]
---
# Implementation guidelines

Prefer small constructor-injected components with one responsibility. Keep storage behind `ContextDao`, workflow coordination in services and transport annotations in the MCP adapter. Use immutable records at boundaries and actionable domain errors. Avoid framework abstractions that do not make the talk or workflow clearer.
