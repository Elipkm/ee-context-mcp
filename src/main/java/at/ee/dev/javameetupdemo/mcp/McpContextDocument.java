package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.dto.ContextMetadata;

public record McpContextDocument
        (String id,
         String context,
         ContextMetadata metadata){
}
