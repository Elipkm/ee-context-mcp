# Domain

The engine supports a developer working with a coding assistant on one repository task.

- A **task** is the requested code change.
- A **scope** is the configured repository, current Git branch and active feature.
- A **context map** is the Markdown index describing available knowledge and when it matters.
- A **context plan** is the selected file-path manifest with inclusion and exclusion reasons.
- A **proposal** is a validated, reviewable set of living-context changes that has not yet been applied.

The coding assistant owns code changes and test execution. The engine owns context discovery, scope enforcement and the review/apply lifecycle. The developer owns approval.
