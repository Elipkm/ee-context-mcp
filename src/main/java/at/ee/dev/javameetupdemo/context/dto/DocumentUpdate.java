package at.ee.dev.javameetupdemo.context.dto;

import at.ee.dev.javameetupdemo.context.enumm.Tag;

import java.util.Set;

public record DocumentUpdate(
        String id,
        String expectedVersion,
        String markdown,
        Set<Tag> tags) {
}
