---
id: testing-guidelines
scope: GLOBAL
branch:
tags: [TEST]
---
# Testing guidelines

Tests should prove observable workflow rules: exact branch filtering, global-document eligibility, OR tag matching, full replacement and stale-version rejection. Keep tests fast with temporary repositories and add a Spring context test to catch MCP wiring and configuration errors.
