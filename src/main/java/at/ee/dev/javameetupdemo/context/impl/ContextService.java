package at.ee.dev.javameetupdemo.context.impl;

import at.ee.dev.javameetupdemo.context.api.IContextDao;
import at.ee.dev.javameetupdemo.context.api.IContextService;
import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.context.dto.ContextMetadata;
import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;
import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.enumm.ContextLoad;
import at.ee.dev.javameetupdemo.mcp.GetContextInput;
import at.ee.dev.javameetupdemo.mcp.UpdateContextInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ContextService implements IContextService {

    private static final Logger log = LoggerFactory.getLogger(ContextService.class);

    private final IContextDao contextDao;

    public ContextService(IContextDao contextDao) {
        this.contextDao = contextDao;
    }

    @Override
    public List<McpContextDocument> getContext(GetContextInput input) {
        validate(input);

        List<ContextDocument> documents = contextDao.findAll();
        Set<String> subjects = resolveSubjects(input.subjects(), documents, contextDao.readSubjectAliases());
        Map<String, ContextDocument> allById = new HashMap<>();
        documents.forEach(document -> allById.put(document.id(), document));
        List<ContextDocument> visible = documents.stream()
                .filter(document -> this.isVisibleOnBranch(document, input.branch()))
                .toList();
        List<ContextDocument> matches = visible.stream()
                .filter(document -> document.metadata().subjects().stream().anyMatch(subjects::contains))
                .toList();
        Map<String, ContextDocument> selected = new LinkedHashMap<>();
        visible.stream().filter(document -> document.metadata().load() == ContextLoad.ALWAYS)
                .forEach(document -> selected.put(document.id(), document));
        matches.forEach(document -> selected.put(document.id(), document));
        // Expand the fixed set of subject matches, never the growing result set.
        for (ContextDocument document : matches) {
            for (var relation : document.metadata().relations()) {
                ContextDocument target = allById.get(relation.target());
                if (target == null) {
                    throw new IllegalStateException("Context document " + document.id()
                            + " references missing document " + relation.target());
                }
                if (isVisibleOnBranch(target, input.branch())) {
                    selected.putIfAbsent(target.id(), target);
                }
            }
        }
        List<McpContextDocument> result = selected.values().stream().map(this::toMcpDocument).toList();

        log.info("Selected context documents ids={} branch={} requestedSubjects={} resolvedSubjects={}",
                result.stream().map(McpContextDocument::id).toList(), input.branch(), input.subjects(), subjects);
        return result;
    }

    private Set<String> resolveSubjects(Set<String> requested, List<ContextDocument> documents,
                                        Map<String, List<String>> glossary) {
        Set<String> canonicalSubjects = new TreeSet<>(glossary.keySet());
        documents.forEach(document -> canonicalSubjects.addAll(document.metadata().subjects()));
        Map<String, String> subjectByTerm = new HashMap<>();
        // Reserve canonical identifiers before adding aliases, so aliases cannot shadow them.
        canonicalSubjects.forEach(subject -> registerTerm(subjectByTerm, subject, subject));
        glossary.forEach((subject, aliases) -> aliases.forEach(alias -> registerTerm(subjectByTerm, alias, subject)));

        Set<String> resolved = new TreeSet<>();
        Set<String> unknown = new TreeSet<>();
        for (String term : requested) {
            String subject = subjectByTerm.get(normalizeTerm(term));
            if (subject == null) {
                unknown.add(term);
            } else {
                resolved.add(subject);
            }
        }
        if (!unknown.isEmpty()) {
            List<String> available = canonicalSubjects.stream().limit(20)
                    .map(subject -> subject + (glossary.getOrDefault(subject, List.of()).isEmpty()
                            ? "" : " (aliases: " + String.join(", ", glossary.get(subject)) + ")"))
                    .toList();
            throw new IllegalArgumentException("Unknown subjects: " + unknown + ". Available subjects"
                    + (canonicalSubjects.size() > 20 ? " (first 20)" : "") + ": " + available
                    + ". Use a listed subject or alias; maintain synonyms in engineering-context/subjects.yaml.");
        }
        return resolved;
    }

    private void registerTerm(Map<String, String> subjectByTerm, String term, String subject) {
        String previous = subjectByTerm.putIfAbsent(normalizeTerm(term), subject);
        if (previous != null && !previous.equals(subject)) {
            throw new IllegalArgumentException("Ambiguous subject alias '" + term + "': " + previous
                    + " and " + subject + ". Each glossary alias must identify one subject.");
        }
    }

    private String normalizeTerm(String term) {
        return term.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public synchronized List<McpContextDocument> updateContext(UpdateContextInput input) {
        validate(input);

        Map<String, ContextDocument> storedById = new HashMap<>();
        contextDao.findAll().forEach(document -> storedById.put(document.id(), document));

        Set<String> requestedIds = new HashSet<>();
        List<ContextDocument> replacements = input.documents().stream()
                .map(update -> this.prepareReplacement(input.branch(), update, storedById, requestedIds))
                .toList();

        List<McpContextDocument> result = replacements.stream()
                .map(contextDao::write)
                .map(this::toMcpDocument)
                .toList();

        log.info("Updated {} context documents for branch={} reason='{}'",
                result.size(), input.branch(), input.descriptionShort());
        return result;
    }

    private ContextDocument prepareReplacement(
            String branch,
            McpContextDocument update,
            Map<String, ContextDocument> storedById,
            Set<String> requestedIds) {
        if (update == null || isBlank(update.id()) || isBlank(update.context())) {
            throw new RuntimeException("Every context document needs an id and context");
        }
        if (!update.id().matches("[a-z0-9][a-z0-9-]*")) {
            throw new RuntimeException("Context document id must contain only lowercase letters, numbers and hyphens");
        }
        validateMetadata(update.id(), update.metadata());
        if (!requestedIds.add(update.id())) {
            throw new RuntimeException("Context document requested more than once: " + update.id());
        }

        ContextDocument stored = storedById.get(update.id());
        if (stored != null && !isVisibleOnBranch(stored, branch)) {
            throw new RuntimeException("Context document " + update.id() + " does not belong to branch " + branch);
        }

        ContextMetadata metadata = update.metadata();
        String documentBranch = metadata.scope() == ContextScope.GLOBAL ? null : branch;
        return new ContextDocument(
                update.id(), stored == null ? "engineering-context/" + update.id() + ".md" : stored.path(),
                update.context(), stored == null ? null : stored.version(), documentBranch, metadata);
    }

    private McpContextDocument toMcpDocument(ContextDocument document) {
        return new McpContextDocument(document.id(), document.markdown(), document.metadata());
    }

    private boolean isVisibleOnBranch(ContextDocument document, String branch) {
        return document.metadata().scope() == ContextScope.GLOBAL || branch.equals(document.branch());
    }

    private void validate(GetContextInput input) {
        if (input == null || isBlank(input.task()) || isBlank(input.descriptionShort()) || isBlank(input.branch())) {
            throw new RuntimeException("task, descriptionShort and branch are required");
        }
        if (input.subjects() == null || input.subjects().isEmpty()) {
            throw new RuntimeException("At least one subject is required");
        }
        for (String subject : input.subjects()) {
            if (isBlank(subject)) {
                throw new RuntimeException("Each subject must be a non-blank term or canonical identifier");
            }
        }
    }

    private void validate(UpdateContextInput input) {
        if (input == null || isBlank(input.descriptionShort()) || isBlank(input.branch())) {
            throw new RuntimeException("descriptionShort and branch are required");
        }
        if (input.documents() == null || input.documents().isEmpty()) {
            throw new RuntimeException("At least one context document update is required");
        }
    }

    private void validateMetadata(String id, ContextMetadata metadata) {
        if (metadata == null || metadata.scope() == null) {
            throw new RuntimeException("Context document " + id + " needs a scope");
        }
        if (metadata.tags() == null || metadata.tags().isEmpty()) {
            throw new RuntimeException("At least one tag is required for context document " + id);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
