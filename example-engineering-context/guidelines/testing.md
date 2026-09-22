---
id: testing-guidelines
scope: GLOBAL
branch:
tags: [TEST]
subjects: [use-case:get-context, use-case:update-context]
---
# Testing guidelines

Tests should prove observable workflow rules: exact branch filtering, global-document eligibility, OR subject matching, baseline inclusion, one-hop relation expansion, full replacement and stale-version rejection. Keep tests fast with temporary repositories and add a Spring context test to catch MCP wiring and configuration errors.
