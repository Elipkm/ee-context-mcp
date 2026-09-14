package at.ee.dev.javameetupdemo.context.impl;

import at.ee.dev.javameetupdemo.context.api.IContextDao;
import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.context.enumm.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
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
            ensureUniqueIds(documents);
            log.info("Loaded {} context documents from {}", documents.size(), contextDirectory);
            return documents;
        } catch (IOException exception) {
            throw new RuntimeException("Could not list context documents", exception);
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
            return new ContextDocument(document.id(), document.path(), document.markdown(), document.expectedVersion(),
                    hash(fileContent), document.scope(), document.branch(), Set.copyOf(document.tags()));
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

            String metadata = normalized.substring(4, metadataEnd);
            String markdown = normalized.substring(metadataEnd + 5);
            String id = metadataValue(metadata, "id", path);
            ContextScope scope = parseScope(metadataValue(metadata, "scope", path), path);
            String branch = optionalMetadataValue(metadata, "branch");
            Set<Tag> tags = parseTags(metadataValue(metadata, "tags", path), path);
            validateMetadata(path, id, scope, branch, tags);

            String relativePath = repositoryRoot.relativize(path).toString().replace('\\', '/');
            return new ContextDocument(id, relativePath, markdown, null, hash(fileContent), scope, branch, tags);
        } catch (IOException exception) {
            throw new RuntimeException("Could not read context document " + path, exception);
        }
    }

    private String serialize(ContextDocument document) {
        String branch = document.scope() == ContextScope.BRANCH ? document.branch() : "";
        String tags = document.tags().stream()
                .map(Enum::name)
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        String markdown = document.markdown().replace("\r\n", "\n");
        if (!markdown.endsWith("\n")) {
            markdown += "\n";
        }
        return """
                ---
                id: %s
                scope: %s
                branch: %s
                tags: [%s]
                ---
                %s""".formatted(document.id(), document.scope(), branch, tags, markdown);
    }

    private String metadataValue(String metadata, String key, Path path) {
        String value = optionalMetadataValue(metadata, key);
        if (value == null || value.isBlank()) {
            throw invalid(path, "missing metadata field '" + key + "'");
        }
        return value;
    }

    private String optionalMetadataValue(String metadata, String key) {
        String prefix = key + ":";
        return metadata.lines()
                .map(String::trim)
                .filter(line -> line.startsWith(prefix))
                .map(line -> line.substring(prefix.length()).trim())
                .findFirst()
                .orElse(null);
    }

    private ContextScope parseScope(String value, Path path) {
        try {
            return ContextScope.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid(path, "unknown scope '" + value + "'");
        }
    }

    private Set<Tag> parseTags(String value, Path path) {
        if (!value.startsWith("[") || !value.endsWith("]")) {
            throw invalid(path, "tags must use [TAG, TAG] format");
        }
        String content = value.substring(1, value.length() - 1).trim();
        if (content.isEmpty()) {
            return Set.of();
        }
        Set<Tag> tags = new LinkedHashSet<>();
        for (String valuePart : content.split(",")) {
            try {
                tags.add(Tag.valueOf(valuePart.trim()));
            } catch (IllegalArgumentException exception) {
                throw invalid(path, "unknown tag '" + valuePart.trim() + "'");
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
