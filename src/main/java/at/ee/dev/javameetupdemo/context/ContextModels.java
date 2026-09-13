package at.ee.dev.javameetupdemo.context;

import java.util.List;
import java.util.Set;

public final class ContextModels {

    private ContextModels() {
    }

    public enum ContextScope {
        GLOBAL,
        BRANCH
    }

    public enum Tag {
        FEATURE,
        BUG,
        IMPL,
        TEST,
        ARCHITECTURE,
        DOMAIN,
        BUSINESS_RULE,
        DOCUMENTATION
    }

    public record GetContextInput(
            String task,
            String descriptionShort,
            String branch,
            Set<Tag> tags) {
    }

    public record ContextDocument(
            String id,
            String markdown,
            String version,
            Set<Tag> tags) {
    }

    public record UpdateContextInput(
            String descriptionShort,
            String branch,
            List<DocumentUpdate> documents) {
    }

    public record DocumentUpdate(
            String id,
            String expectedVersion,
            String markdown,
            Set<Tag> tags) {
    }

    public record UpdatedDocument(String id, String version) {
    }

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
}
