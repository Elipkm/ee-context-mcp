# Personal Developer Context Engine

The Personal Developer Context Engine is a deliberately small Spring Boot demo for a Java meetup. It provides coding assistants such as Codex or Claude with repository-specific engineering context through MCP.

The central idea is simple:

> Access to a repository is not the same as receiving the context relevant to the current task.

## Responsibilities

- Markdown documents contain engineering knowledge and metadata.
- Git provides branches, versioning and history.
- The context engine performs deterministic scope and tag filtering.
- MCP carries structured requests and complete context documents.
- The coding assistant chooses the relevant tags, reasons over the returned Markdown and implements the task.

## Workflow

1. The assistant receives an implementation task.
2. It calls `get_context` with the exact branch and relevant tags.
3. The engine returns matching documents with stable IDs and metadata.
4. The assistant implements and tests the change.
5. The assistant performs a brief check for new, durable and non-duplicative engineering knowledge. Explicit developer corrections and reusable coding instructions are strong candidates; routine task details are not.
6. When qualifying knowledge exists, the assistant shows the developer an exact minimal diff for review.
7. After explicit approval, the assistant calls `update_context` with only the approved complete documents to create or replace by ID.
8. The engine validates and writes the documents, including when the context store is empty.

## Selection rules

A document is eligible when it is global or belongs to the exact requested branch. Among eligible documents, tags use OR matching. Global documents are not included automatically.

This keeps the engine predictable and leaves semantic selection with the assistant.

## Storage

The demo stores context as Markdown with YAML front matter below `engineering-context/`. A DAO keeps filesystem storage replaceable. A larger organization could provide the same contract over Confluence, Jira, a database or another managed source.

Scaling the storage would add governance concerns such as ownership, permissions, freshness, auditability and conflict resolution. It would not change the two-tool interaction model.
