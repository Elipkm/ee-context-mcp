package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record UpdateContextInput(
        @JsonPropertyDescription("Concise reason the durable engineering context changed, including an explicit developer correction or reusable rule; not a task-completion summary")
        String descriptionShort,
        @JsonPropertyDescription("Exact Git branch used to validate document visibility")
        String branch,
        @JsonPropertyDescription("Minimal set of developer-approved complete documents to create or replace; omit unchanged documents")
        List<McpContextDocument> documents) {
}
