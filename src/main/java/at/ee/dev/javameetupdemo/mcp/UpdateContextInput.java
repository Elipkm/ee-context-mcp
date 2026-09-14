package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.dto.ContextDocument;

import java.util.List;

public record UpdateContextInput(
        String descriptionShort,
        String branch,
        List<ContextDocument> documents) {
}
