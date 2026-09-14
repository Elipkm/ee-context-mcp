package at.ee.dev.javameetupdemo.context.dto;

public record ContextDocument(
        String id,
        String path,
        String markdown,
        String version,
        String branch,
        ContextMetadata metadata) {
}
