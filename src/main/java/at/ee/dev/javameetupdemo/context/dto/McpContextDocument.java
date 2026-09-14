package at.ee.dev.javameetupdemo.context.dto;

public record McpContextDocument
        (String id,
         String context,
         ContextMetadata metadata){
}
