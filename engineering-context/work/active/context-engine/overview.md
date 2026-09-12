# Context engine implementation

## Acceptance criteria

- A coding assistant can request a task context plan over MCP.
- The plan reports repository, branch, feature, included paths, reasons and exclusions.
- The assistant can submit living-context replacements and receive a diff without changing files.
- Applying requires an approved proposal ID and refuses stale proposals.
- Local Markdown access is replaceable through `ContextDao`.

## Current decisions

Selection uses a small deterministic policy: baseline technical context plus active-feature and task-keyword matches from the wiki index. Proposal state is in memory, which is sufficient for the single-process demo.
