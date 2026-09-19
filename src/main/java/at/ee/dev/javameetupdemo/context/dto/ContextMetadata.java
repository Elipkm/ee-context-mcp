package at.ee.dev.javameetupdemo.context.dto;

import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.enumm.ContextLoad;
import at.ee.dev.javameetupdemo.context.enumm.Tag;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Set;

public record ContextMetadata(
        @JsonPropertyDescription("Branch validity: GLOBAL is valid on all branches, not automatically loaded")
        ContextScope scope,
        @JsonPropertyDescription("Document classification only; tags do not filter retrieval")
        Set<Tag> tags,
        @JsonPropertyDescription("Exact subject identifiers such as domain:time-tracking, component:booking or use-case:create-booking")
        Set<String> subjects,
        @JsonPropertyDescription("Outgoing links to context document ids; expanded one hop from subject matches only")
        List<ContextRelation> relations,
        @JsonPropertyDescription("SUBJECT_MATCH by default; ALWAYS includes a short baseline document on every get_context call where its branch is valid")
        ContextLoad load
) {
    public ContextMetadata {
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        subjects = subjects == null ? Set.of() : Set.copyOf(subjects);
        relations = relations == null ? List.of() : List.copyOf(relations);
        load = load == null ? ContextLoad.SUBJECT_MATCH : load;
        for (String subject : subjects) {
            if (!subject.matches("[a-z][a-z0-9-]*:[a-z0-9][a-z0-9-]*")) {
                throw new IllegalArgumentException("Subject must use kind:name format, e.g. component:booking");
            }
        }
    }
}
