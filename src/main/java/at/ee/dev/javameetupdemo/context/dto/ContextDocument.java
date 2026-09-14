package at.ee.dev.javameetupdemo.context.dto;

import at.ee.dev.javameetupdemo.context.enumm.Tag;

import java.util.Set;

public record ContextDocument(
        String id,
        String markdown,
        String version,
        Set<Tag> tags
) {}
