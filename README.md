# Personal Developer Context Engine

A small Spring Boot MCP server that gives coding assistants the right repository-local engineering context for a task and lets them propose reviewable documentation updates.

## Core workflow

1. Call `prepare_task_context` with the coding task and optional feature name.
2. Read each returned Markdown path once, then implement and test the change.
3. Call `propose_context_updates` with complete replacement content for changed files below `engineering-context/work/`.
4. Show the returned diff to the developer.
5. Only after explicit approval, call `apply_context_updates` with the proposal ID and `approved: true`.

The server deliberately returns paths instead of duplicating local file contents through MCP. Scope is restricted to the configured repository and context writes are restricted to living work documentation.

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

Override the repository if the process starts elsewhere:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--context-engine.repository-root=C:\path\to\repository"
```

HTTP MCP transports have no authentication in this demo, so the server binds to localhost only.
