# Personal Developer Context Engine

A small Spring Boot MCP server that gives a coding assistant tagged, repository-local engineering context.

## The two MCP tools

1. `get_context` receives the task, a short description, the exact Git branch and the tags chosen by the assistant.
2. `update_context` replaces existing documents by ID using their context and metadata.

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

For using add to your working Repository AGENTS.md the following instructions:
    """use developer-context mcp server to manage and get relevant context
        before starting the task get relevant context by calling the get_context tool
        when finished give a brief summary what context must be persisted and 
        after user confirmation call tool update_context"""
