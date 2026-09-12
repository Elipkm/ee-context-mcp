# Testing guidelines

Tests should prove observable workflow rules: deterministic context selection, scope rejection, path validation, explicit approval, stale-proposal rejection and successful writes. Keep tests fast with temporary repositories and add a Spring context test to catch MCP wiring and configuration errors.
