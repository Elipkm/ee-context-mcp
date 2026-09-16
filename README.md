# Personal Developer Context Engine

A small Spring Boot MCP server that gives a coding assistant tagged, repository-local engineering context.

## The two MCP tools

1. `get_context` receives the task, a short description, the exact Git branch and the tags chosen by the assistant.
2. `update_context` creates documents for new IDs or fully replaces existing documents by ID using their context and metadata.

New documents are stored at `engineering-context/<id>.md`. IDs must contain only lowercase letters, numbers and hyphens, starting with a letter or number. Branch-scoped documents use the exact branch from the request; global documents have no branch. Existing documents keep their file path and must be visible on the requested branch. Each batch is validated before any documents are written.

Retrieval uses two simple rules:

- Global documents and documents for the exact requested branch are eligible.
- A document is returned when any requested tag matches.

Each result contains the complete Markdown body, stable document ID and metadata. The assistant decides how to use the returned documents.

## Context document format

Context lives below `engineering-context/` and uses small YAML front matter:

```markdown
---
id: order-cancellation
scope: BRANCH
branch: feature/order-cancellation
tags: [BUSINESS_RULE, FEATURE, TEST]
---
# Order cancellation

Complete context goes here.
```

Use `scope: GLOBAL` with an empty `branch:` for context that can apply to every branch. Global context is still returned only when one of its tags is requested.

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

The stateless Streamable HTTP MCP endpoint is `http://127.0.0.1:8080/mcp`.

Example Codex configuration:

```toml
[mcp_servers.developer-context]
url = "http://127.0.0.1:8080/mcp"
```

Set the repository in `application.yaml` or override it when starting the application:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--context-engine.repository-root=C:\path\to\repository"
```

HTTP MCP transport has no authentication in this demo, so the server binds to localhost only.

## Recommended repository instructions

Add the following to the working repository's `AGENTS.md`:

```markdown
Use the developer-context MCP server to manage relevant engineering context.

Before starting a task, call `get_context` with the exact Git branch and the
relevant tags.

After completing a task, perform a brief context check using the context already
retrieved for the task. Treat an explicit developer correction or reusable
instruction about how code must, must not or should be written as a strong
context candidate. This includes preferences expressed while correcting an
implementation, even when the resulting code now demonstrates the preferred
pattern. Also check for new or changed architectural decisions, business rules,
constraints, rationale and unresolved risks that will materially affect future
work.

If not, state "No durable context change" and stop. Do not call
`update_context`.

If yes, show the developer the exact minimal context diff. For every change,
state why it will help future work, why it is not already covered and which
document should own it. Distinguish a rule from the implementation that exposed
it: save the reusable rule, not the refactoring summary. Do not rescan the
repository, repeat canonical documentation, or include task history,
implementation inventories, setup details, transient status or test results.
Prefer updating one existing canonical document and keep the proposal below
150 words.

Example: if the developer says "never use native queries, and never query from
a service", propose adding that repository-wide persistence rule to the
canonical architecture or Java guidelines. Do not save the list of classes
changed during the refactoring.

Call `update_context` only after the developer has reviewed and explicitly
approved the exact content. Submit exactly the approved content.
```
