package at.ee.dev.javameetupdemo.context.dto;

import at.ee.dev.javameetupdemo.context.enumm.Tag;

import java.util.Set;

public record ContextDocument(
        String id,
        String path,
        String markdown,
        String expectedVersion,
        String version,
        String branch,
        ContextMetadata metadata) {

    public static ContextDocument update(String id, String expectedVersion, String markdown, Set<Tag> tags) {
        return new ContextDocument(id, null, markdown, expectedVersion, null, null, new ContextMetadata(null, tags));
    }
}
