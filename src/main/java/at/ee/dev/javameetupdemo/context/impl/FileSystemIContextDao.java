package at.ee.dev.javameetupdemo.context.impl;

import at.ee.dev.javameetupdemo.context.api.IContextDao;
import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.context.dto.ContextMetadata;
import at.ee.dev.javameetupdemo.context.dto.ContextRelation;
import at.ee.dev.javameetupdemo.context.enumm.ContextLoad;
import at.ee.dev.javameetupdemo.context.enumm.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
public class FileSystemIContextDao implements IContextDao {

    private static final Logger log = LoggerFactory.getLogger(FileSystemIContextDao.class);
    private static final String CONTEXT_DIRECTORY = "engineering-context";
    private static final String FRONT_MATTER_SEPARATOR = "---";

    private final Path repositoryRoot;

    public FileSystemIContextDao(@Value("${context-engine.repository-root}") String repositoryRoot) {
        this.repositoryRoot = Path.of(repositoryRoot).toAbsolutePath().normalize();
    }

    @Override
    public List<ContextDocument> findAll() {
        Path contextDirectory = resolve(CONTEXT_DIRECTORY);
        if (!Files.isDirectory(contextDirectory)) {
            log.info("No context directory found at {}", contextDirectory);
            return List.of();
        }

        try (var files = Files.walk(contextDirectory)) {
            List<ContextDocument> documents = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .map(this::read)
                    .toList();
            this.ensureUniqueIds(documents);
            log.info("Loaded {} context documents from {}", documents.size(), contextDirectory);
            return documents;
        } catch (IOException exception) {
            throw new RuntimeException("Could not list context documents", exception);
        }
    }

    @Override
    public Map<String, List<String>> readSubjectAliases() {
        Path path = resolve(CONTEXT_DIRECTORY + "/subjects.yaml");
        if (!Files.exists(path)) {
            return Map.of();
        }
        try {
            Map<String, Object> glossary = parseMetadata(Files.readString(path), path);
            if (!(glossary.get("subjects") instanceof Map<?, ?> subjects)) {
                throw invalid(path, "glossary needs a subjects mapping");
            }
            Map<String, List<String>> aliases = new LinkedHashMap<>();
            for (var entry : subjects.entrySet()) {
                String subject = stringValue(entry.getKey(), "subject", path);
                if (!subject.matches("[a-z][a-z0-9-]*:[a-z0-9][a-z0-9-]*")) {
                    throw invalid(path, "glossary subject must use kind:name format: " + subject);
                }
                if (!(entry.getValue() instanceof Map<?, ?> details)
                        || !(details.get("aliases") instanceof List<?> terms)) {
                    throw invalid(path, "subject " + subject + " needs an aliases list");
                }
                List<String> names = terms.stream().map(term -> stringValue(term, "alias", path)).toList();
                if (names.stream().anyMatch(String::isBlank)) {
                    throw invalid(path, "subject " + subject + " has a blank alias");
                }
                aliases.put(subject, names);
            }
            return Map.copyOf(aliases);
        } catch (IOException exception) {
            throw new RuntimeException("Could not read subject glossary " + path, exception);
        }
    }

    @Override
    public ContextDocument write(ContextDocument document) {
        Path path = resolve(document.path());
        String fileContent = serialize(document);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, fileContent);
            log.info("Updated context document id={} path={}", document.id(), document.path());
            return new ContextDocument(document.id(), document.path(), normalizeMarkdown(document.markdown()), hash(fileContent),
                    document.branch(), document.metadata());
        } catch (IOException exception) {
            throw new RuntimeException("Could not write context document " + document.id(), exception);
        }
    }

    private ContextDocument read(Path path) {
        try {
            String fileContent = Files.readString(path);
            String normalized = fileContent.replace("\r\n", "\n");
            if (!normalized.startsWith(FRONT_MATTER_SEPARATOR + "\n")) {
                throw invalid(path, "missing YAML front matter");
            }

            int metadataEnd = normalized.indexOf("\n" + FRONT_MATTER_SEPARATOR + "\n", 4);
            if (metadataEnd < 0) {
                throw invalid(path, "front matter is not closed");
            }

            Map<String, Object> metadata = parseMetadata(normalized.substring(4, metadataEnd), path);
            String markdown = normalized.substring(metadataEnd + 5);
            String id = stringValue(metadata.get("id"), "id", path);
            ContextScope scope = parseScope(stringValue(metadata.get("scope"), "scope", path), path);
            String branch = metadata.get("branch") == null ? "" : stringValue(metadata.get("branch"), "branch", path);
            Set<Tag> tags = parseTags(listValue(metadata, "tags", path), path);
            Set<String> subjects = new LinkedHashSet<>();
            for (Object subject : listValue(metadata, "subjects", path)) {
                subjects.add(stringValue(subject, "subjects", path));
            }
            List<ContextRelation> relations = listValue(metadata, "relations", path).stream()
                    .map(value -> parseRelation(value, path)).toList();
            ContextLoad load = parseLoad(metadata.get("load"), path);
            validateMetadata(path, id, scope, branch, tags);

            String relativePath = repositoryRoot.relativize(path).toString().replace('\\', '/');
            return new ContextDocument(id, relativePath, markdown, hash(fileContent), branch,
                    new ContextMetadata(scope, tags, subjects, relations, load));
        } catch (IOException exception) {
            throw new RuntimeException("Could not read context document " + path, exception);
        }
    }

    private String serialize(ContextDocument document) {
        String branch = document.metadata().scope() == ContextScope.BRANCH ? document.branch() : "";
        String tags = document.metadata().tags().stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        String markdown = normalizeMarkdown(document.markdown());
        StringBuilder extra = new StringBuilder("load: " + document.metadata().load() + "\n");
        if (!document.metadata().subjects().isEmpty()) {
            extra.append("subjects:\n");
            document.metadata().subjects().stream().sorted()
                    .forEach(subject -> extra.append("  - ").append(subject).append('\n'));
        }
        if (!document.metadata().relations().isEmpty()) {
            extra.append("relations:\n");
            document.metadata().relations().forEach(relation -> extra.append("  - type: ")
                    .append(relation.type()).append("\n    target: ").append(relation.target()).append('\n'));
        }
        return """
                ---
                id: %s
                scope: %s
                branch: %s
                tags: [%s]
                %s---
                %s""".formatted(document.id(), document.metadata().scope(), branch, tags, extra, markdown);
    }

    private String normalizeMarkdown(String markdown) {
        String normalized = markdown.replace("\r\n", "\n");
        return normalized.endsWith("\n") ? normalized : normalized + "\n";
    }

    private Map<String, Object> parseMetadata(String yaml, Path path) {
        try {
            var factory = new YamlMapFactoryBean();
            factory.setResources(new ByteArrayResource(yaml.getBytes(StandardCharsets.UTF_8)));
            return factory.getObject();
        } catch (RuntimeException exception) {
            throw invalid(path, "could not parse YAML: " + exception.getMessage());
        }
    }

    private String stringValue(Object value, String field, Path path) {
        if (!(value instanceof String text)) {
            throw invalid(path, "metadata field '" + field + "' must be a string");
        }
        return text;
    }

    private List<?> listValue(Map<String, Object> metadata, String field, Path path) {
        if (!metadata.containsKey(field)) {
            return List.of();
        }
        if (!(metadata.get(field) instanceof List<?> values)) {
            throw invalid(path, "metadata field '" + field + "' must be a YAML list");
        }
        return values;
    }

    private ContextRelation parseRelation(Object value, Path path) {
        if (!(value instanceof Map<?, ?> relation)) {
            throw invalid(path, "each relation must have a type and target");
        }
        return new ContextRelation(stringValue(relation.get("type"), "relation.type", path),
                stringValue(relation.get("target"), "relation.target", path));
    }

    private ContextLoad parseLoad(Object value, Path path) {
        if (value == null) {
            return ContextLoad.SUBJECT_MATCH;
        }
        try {
            return ContextLoad.valueOf(stringValue(value, "load", path));
        } catch (IllegalArgumentException exception) {
            throw invalid(path, "unknown load policy '" + value + "'");
        }
    }

    private ContextScope parseScope(String value, Path path) {
        try {
            return ContextScope.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid(path, "unknown scope '" + value + "'");
        }
    }

    private Set<Tag> parseTags(List<?> values, Path path) {
        Set<Tag> tags = new LinkedHashSet<>();
        for (Object value : values) {
            String name = stringValue(value, "tags", path);
            try {
                tags.add(Tag.valueOf(name));
            } catch (IllegalArgumentException exception) {
                throw invalid(path, "unknown tag '" + name + "'");
            }
        }
        return Set.copyOf(tags);
    }

    private void validateMetadata(Path path, String id, ContextScope scope, String branch, Set<Tag> tags) {
        if (!id.matches("[a-z0-9][a-z0-9-]*")) {
            throw invalid(path, "id must contain only lowercase letters, numbers and hyphens");
        }
        if (scope == ContextScope.BRANCH && (branch == null || branch.isBlank())) {
            throw invalid(path, "branch-scoped documents need a branch");
        }
        if (scope == ContextScope.GLOBAL && branch != null && !branch.isBlank()) {
            throw invalid(path, "global documents must not declare a branch");
        }
        if (tags.isEmpty()) {
            throw invalid(path, "at least one tag is required");
        }
    }

    private void ensureUniqueIds(List<ContextDocument> documents) {
        Set<String> ids = new HashSet<>();
        documents.forEach(document -> {
            if (!ids.add(document.id())) {
                throw new RuntimeException("Duplicate context document id: " + document.id());
            }
        });
    }

    private RuntimeException invalid(Path path, String reason) {
        return new RuntimeException("Invalid context document " + path + ": " + reason);
    }

    private Path resolve(String relativePath) {
        Path resolved = repositoryRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(repositoryRoot)) {
            throw new RuntimeException("Path leaves the configured repository: " + relativePath);
        }
        return resolved;
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
