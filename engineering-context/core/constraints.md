# Constraints

- Only the configured repository may be selected.
- Resolved paths must remain below the repository root.
- MCP returns selected paths, not duplicate file content.
- Context writes are limited to `.md` files below `engineering-context/work/`.
- Applying a proposal requires explicit developer approval.
- A proposal must be rejected if a target changed after its diff was produced.
- HTTP access is local-only unless a real authentication boundary is added.
