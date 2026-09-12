

Personal Developer Context Engine

The Personal Developer Context Engine is a Spring Boot demo application that provides coding assistants such as Codex or Claude with relevant, repository-specific engineering context through MCP.

Context is stored as transparent Markdown documents, separated into stable core knowledge and living feature documentation. For each development task, the engine identifies the repository and active feature, applies scope boundaries, and uses a wiki-style index to select relevant files. The coding assistant receives their paths, reads them directly, and implements the change with the appropriate context.

After implementation, the assistant can propose updates to the living context. The developer reviews the changes before the engine applies them through an abstract Context DAO.

The demo uses repository-local Markdown, but the same design can later support databases, Confluence, Jira, or other enterprise context sources without changing the central workflow.


engineering-context/
├── README.md
├── domain.md
├── business-goals.md
├── architecture.md
├── tech-stack.md
├── constraints.md
├── progress.md
|--- help.md
└── guidelines/
├── implementation.md
├── documentation.md
└── testing.md


That connects directly to your thesis: AI works better when intent, constraints, architecture, and expectations are explicit.

You should therefore introduce it as a deliberately small **personal reference implementation**:

> In a company, this context could come from Confluence, ADRs, Jira, Git history, API specifications, or other systems. For this demo, Markdown keeps the mechanism transparent.



Living work context objects (per feature)
## Strong connection to Git

Ideally, the living objects are committed alongside the corresponding code:

```
feat: support order cancellation

- reject cancellation after shipment
- refund paid orders
- release reserved inventory
- add cancellation test scenarios
- update order lifecycle documentation
```

The commit then contains:

- implementation changes
- tests
- documentation
- updated context objects

This supports a particularly strong message for your talk:

> A change is not complete when the code is written. It is complete when its implementation, verification, documentation, and intent tell the same story.

That makes the context engine more than a Markdown manager. It becomes a lightweight personal workflow for maintaining engineering intent throughout the lifecycle of a change.

Engine Context Selection
The selection and assembly logic remains independent of storage:

```
Context sources → selector → validator → assembler → context package
```

## Your scalability argument

Your argument is valid, with one useful nuance:

> The underlying idea remains the same regardless of whether context is stored in Markdown, Confluence, or a database. What changes at scale is the required governance.

For a personal repository, Markdown and Git provide:

- transparency
- versioning
- portability
- direct AI access
- almost no infrastructure

In a larger organization, you additionally need:

- ownership
- access control
- freshness guarantees
- auditability
- conflict resolution
- ranking across many sources
- integration with existing systems


> “Why do I need an engine if the AI can already read the repository?”

Your response:

> “Access to information is not the same as access to the right context. The engine curates, validates and explains the context selected for the task.”


Context Selection Layers levels:
### 1. Hard scope filtering

The engine establishes safe boundaries using deterministic information:

- current repository
- current Git branch
- active feature
- document status
- access permissions
### 2. Wiki index as a navigation map

The assistant initially receives a compact index describing what knowledge exists and when it is useful.
### 3. Context manifest

The engine records what was ultimately used:

```
task: Implement order cancellation
feature: order-cancellation
branch: feature/order-cancellation

included:
  - path: core/domain.md
    reason: Order lifecycle
  - path: core/constraints.md
    reason: Cancellation restrictions
  - path: core/architecture.md
    reason: Payment and inventory boundaries
  - path: work/active/order-cancellation/testing.md
    reason: Expected behavior
```


SO Usage of Engine:
- important selected context is transparent
- AI Agent uses engine itself automatically by connecting enginges mcp endpoints for read/write



This creates a clear separation:

- **Markdown files:** knowledge
- **Git:** versioning and history
- **Context engine:** selection, validation and lifecycle
- **MCP:** communication between the engine and coding assistant
- **Coding assistant:** implementation and reasoning

The coding assistant performs the workflow:

```
1. Receive implementation task
2. Call context engine through MCP
3. Receive selected repository paths
4. Read those files once from the repository
5. Implement and test the change
6. Propose context updates through MCP
7. Ask for approval before applying them
```

A strong phrase for the talk would be:

> Don’t give the AI every document. Give it clear boundaries, a map of the available knowledge, and the ability to explore within that space.

This is more interesting than a simple file loader because it demonstrates context engineering as a combination of **scope, navigation, discovery, and evidence**.
Note: Keep in mind cross cutting concerns of featuers