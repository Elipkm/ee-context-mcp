package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.enumm.Tag;

import java.util.Set;

public record GetContextInput(
        String task,
        String descriptionShort,
        String branch,
        Set<Tag> tags) {
}
