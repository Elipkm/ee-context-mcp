package at.ee.dev.javameetupdemo.context.dto;

import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.enumm.Tag;

import java.util.Set;

public record ContextMetadata(
        ContextScope scope,
        Set<Tag> tags
) {
}
