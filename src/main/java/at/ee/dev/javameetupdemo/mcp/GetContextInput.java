package at.ee.dev.javameetupdemo.mcp;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public record GetContextInput(
        String task,
        String descriptionShort,
        String branch,
        @JsonProperty(value = "subjects", required = true)
        @JsonPropertyDescription("At least one short subject term derived from the task, e.g. booking import. The server resolves glossary aliases ignoring case and surrounding whitespace. Known canonical identifiers also work. Unknown terms return an error with available subjects and aliases; no separate discovery call is needed.")
        Set<String> subjects) {
}
