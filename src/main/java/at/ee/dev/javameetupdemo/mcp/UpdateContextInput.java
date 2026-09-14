package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;

import java.util.List;

public record UpdateContextInput(
        String descriptionShort,
        String branch,
        List<McpContextDocument> documents) {
}
