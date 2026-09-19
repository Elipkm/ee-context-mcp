package at.ee.dev.javameetupdemo.context.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record McpContextDocument
        (@JsonPropertyDescription("Stable canonical topic id; reuse an existing id instead of creating a duplicate topic")
         String id,
         @JsonPropertyDescription("Concise durable knowledge not already documented elsewhere, including explicit developer rules; provide the complete body when replacing a document")
         String context,
         @JsonPropertyDescription("Branch validity, classification tags, subjects, outgoing document relations and load policy; reserve ALWAYS for short baseline context")
         ContextMetadata metadata){
}
