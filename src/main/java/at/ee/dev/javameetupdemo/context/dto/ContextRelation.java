package at.ee.dev.javameetupdemo.context.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ContextRelation(
        @JsonPropertyDescription("Semantic relationship label, e.g. REQUIRES or SHARES_INVARIANT; all explicit outgoing relations are followed for one hop")
        String type,
        @JsonPropertyDescription("Stable id of the related context document, not a subject or file path")
        String target) {

    public ContextRelation {
        if (type == null || !type.matches("[A-Z][A-Z0-9_]*")) {
            throw new IllegalArgumentException("Relation type must be an uppercase label such as REQUIRES");
        }
        if (target == null || !target.matches("[a-z0-9][a-z0-9-]*")) {
            throw new IllegalArgumentException("Relation target must be a context document id");
        }
    }
}
