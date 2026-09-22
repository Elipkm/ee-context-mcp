# ee-context-mcp
idea: central interface for retrieving and persisting domain context

# Personal Developer Context Engine

A small Spring Boot MCP server that gives a coding assistant relevant, repository-local engineering context.

## The two MCP tools

1. `get_context` receives a feature or bug task, a short description, the exact Git branch and at least one simple subject term derived from the task. The context layer resolves its synonym glossary. Call it only when the work needs broader system context; skip routine local edits.
2. `update_context` creates documents for new IDs or fully replaces existing documents by ID using their context and metadata.

New documents are stored at `engineering-context/<id>.md`. IDs must contain only lowercase letters, numbers and hyphens, starting with a letter or number. Branch-scoped documents use the exact branch from the request; global documents have no branch. Existing documents keep their file path and must be visible on the requested branch. Each batch is validated before any documents are written.

Retrieval uses these rules:

First resolve subject terms through the repository glossary (case-insensitive, ignoring surrounding whitespace). Known canonical identifiers also work. Unknown terms return an error listing available subjects and aliases rather than returning partial results or only baseline context.

1. Filter by branch validity: `GLOBAL` is valid on every branch; `BRANCH` requires an exact branch match.
2. Include branch-valid documents marked `load: ALWAYS` as short baseline context.
3. Select documents matching any resolved canonical subject exactly. Tags classify documents and do not filter retrieval.
4. Follow outgoing relations from subject matches for exactly one hop, including only branch-valid targets. Baseline-only documents do not trigger expansion.
5. Return each document once, with baseline documents first, then direct matches, then related documents.

Relations target document IDs. All explicit relation types are followed; the type records the semantic reason for the link. Incoming links and second-hop links are not followed. A missing target on a traversed relation produces an error naming the source and target; a target on another branch is excluded.

Each result contains the complete Markdown body, stable document ID and metadata. The assistant decides how to use the returned documents.

## Context document format

Context lives below `engineering-context/` and uses small YAML front matter:

```markdown
---
id: order-cancellation
scope: BRANCH
branch: feature/order-cancellation
tags: [BUSINESS_RULE, FEATURE, TEST]
load: SUBJECT_MATCH
subjects:
  - domain:orders
  - use-case:cancel-order
relations:
  - type: REQUIRES
    target: cancellation-policy
---
# Order cancellation

Complete context goes here.
```

The example requires a document with ID `cancellation-policy`. Document metadata uses exact lowercase `kind:name` identifiers, for example `domain:time-tracking`, `component:booking` or `use-case:import-bookings`. The agent can supply ordinary glossary terms instead of these identifiers.

`GLOBAL` means valid on every branch, not always loaded. Use `load: ALWAYS` sparingly for a short architecture or overview document. It is included only when the tool is called, and still obeys branch validity.

Omitted `load` defaults to `SUBJECT_MATCH`; omitted `subjects` and `relations` default to empty lists. Existing files remain readable, but documents without subjects need an incoming relation from a selected document or `load: ALWAYS` to be retrieved. Full replacements must include the intended subjects, relations and load policy; omitted fields reset to their defaults.

## Subject glossary

Maintain aliases in `engineering-context/subjects.yaml` in the configured repository. For the order example:

```yaml
subjects:
  use-case:cancel-order:
    aliases:
      - cancel order
      - order cancellation
  use-case:import-bookings:
    aliases:
      - booking import
      - timesheet upload
      - csv import
```

Each alias maps to exactly one canonical subject. The server rejects conflicting aliases, including aliases that shadow another canonical identifier. Case and surrounding whitespace are ignored; wording, internal spacing and punctuation otherwise match literally. This is curated synonym resolution, with no fuzzy guessing or extra discovery tool.

The DAO reads the glossary on each retrieval call, and the context service resolves the terms. An absent glossary is allowed: canonical subjects declared in documents still work. The glossary is maintained as a file; `update_context` updates Markdown documents and does not edit the glossary. Unknown input fails the whole call, with up to 20 available canonical subjects and their aliases in the error. Fix the input or add a deliberate synonym to the glossary.

The included glossary covers this demo's context engine, retrieval, updates, storage and MCP server. When using a different configured repository, maintain its glossary there.

Example `get_context` input (the former `tags` request field is replaced by required `subjects`):

```json
{
  "task": "Add order cancellation",
  "descriptionShort": "Apply cancellation policy across order entry points",
  "branch": "feature/order-cancellation",
  "subjects": ["cancel order"]
}
```

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

Call `get_context` only for feature or bug work that needs broader system
context. Supply the exact Git branch and at least one short subject term derived
from the task, such as "booking import". The server resolves its curated synonym
glossary; canonical identifiers also work. If a term is unknown, use the available
subjects and aliases reported in the error. No discovery call is needed. Skip routine local
edits. Tags describe document kinds and are not retrieval filters. Short
`load: ALWAYS` baseline documents are included whenever the tool is called,
subject to branch validity.

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
