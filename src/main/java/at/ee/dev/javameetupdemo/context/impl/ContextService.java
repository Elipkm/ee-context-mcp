package at.ee.dev.javameetupdemo.context.impl;

import at.ee.dev.javameetupdemo.context.api.IContextDao;
import at.ee.dev.javameetupdemo.context.api.IContextService;
import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.context.dto.ContextMetadata;
import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;
import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.context.enumm.Tag;
import at.ee.dev.javameetupdemo.mcp.GetContextInput;
import at.ee.dev.javameetupdemo.mcp.UpdateContextInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        List<McpContextDocument> result = contextDao.findAll().stream()
                .filter(document -> this.isVisibleOnBranch(document, input.branch()))
                .filter(document -> this.hasAnyTag(document, input.tags()))
                .map(this::toMcpDocument)
                .toList();

        log.info("Selected context documents ids={} branch={} tags={}",
                result.stream().map(McpContextDocument::id).toList(), input.branch(), input.tags());
        return result;
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
        validateMetadata(update.id(), update.metadata());
        if (!requestedIds.add(update.id())) {
            throw new RuntimeException("Context document requested more than once: " + update.id());
        }

        ContextDocument stored = storedById.get(update.id());
        if (stored == null) {
            throw new RuntimeException("Unknown context document id: " + update.id());
        }
        if (!isVisibleOnBranch(stored, branch)) {
            throw new RuntimeException("Context document " + update.id() + " does not belong to branch " + branch);
        }

        ContextMetadata metadata = new ContextMetadata(
                update.metadata().scope(), Set.copyOf(update.metadata().tags()));
        String documentBranch = metadata.scope() == ContextScope.GLOBAL ? null : branch;
        return new ContextDocument(
                stored.id(), stored.path(), update.context(), stored.version(), documentBranch, metadata);
    }

    private McpContextDocument toMcpDocument(ContextDocument document) {
        return new McpContextDocument(document.id(), document.markdown(), document.metadata());
    }

    private boolean isVisibleOnBranch(ContextDocument document, String branch) {
        return document.metadata().scope() == ContextScope.GLOBAL || branch.equals(document.branch());
    }

    private boolean hasAnyTag(ContextDocument document, Set<Tag> requestedTags) {
        return document.metadata().tags().stream().anyMatch(requestedTags::contains);
    }

    private void validate(GetContextInput input) {
        if (input == null || isBlank(input.task()) || isBlank(input.descriptionShort()) || isBlank(input.branch())) {
            throw new RuntimeException("task, descriptionShort and branch are required");
        }
        if (input.tags() == null || input.tags().isEmpty()) {
            throw new RuntimeException("At least one context tag is required");
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
