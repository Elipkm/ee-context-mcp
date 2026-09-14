package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.dto.DocumentUpdate;

import java.util.List;

public record UpdateContextInput(
        String descriptionShort,
        String branch,
        List<DocumentUpdate> documents) {
}
