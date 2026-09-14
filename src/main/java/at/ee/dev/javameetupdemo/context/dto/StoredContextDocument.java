package at.ee.dev.javameetupdemo.context.dto;

import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.enumm.Tag;

import java.util.Set;

public record StoredContextDocument(
        String id,
        String path,
        String markdown,
        String version,
        ContextScope scope,
        String branch,
        Set<Tag> tags) {

    public ContextDocument toContextDocument() {
        return new ContextDocument(id, markdown, version, tags);
    }
}
