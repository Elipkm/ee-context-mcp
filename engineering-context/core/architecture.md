# Architecture

MCP is the control plane. It exposes three tools: prepare task context, propose context updates and apply an approved proposal.

The task orchestrator asks the scope resolver for the repository, branch and feature. The selector navigates `engineering-context/index.md`, includes baseline engineering documents, adds matching feature or cross-cutting context, and reports exclusions. It returns paths rather than file content.

The update manager validates proposed complete-file replacements, builds a reviewable diff and retains their original hashes. Apply rechecks those hashes to prevent overwriting context changed since review. All storage access crosses the `ContextDao` interface.
