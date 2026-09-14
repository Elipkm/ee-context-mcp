package at.ee.dev.javameetupdemo.context.impl;

import at.ee.dev.javameetupdemo.context.api.IContextDao;
import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
import at.ee.dev.javameetupdemo.mcp.GetContextInput;
import at.ee.dev.javameetupdemo.context.enumm.Tag;
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
public class ContextService {

    private static final Logger log = LoggerFactory.getLogger(ContextService.class);

    private final IContextDao IContextDao;

    public ContextService(IContextDao IContextDao) {
        this.IContextDao = IContextDao;
    }

    public List<ContextDocument> getContext(GetContextInput input) {
        validate(input);

        List<ContextDocument> result = IContextDao.findAll().stream()
                .filter(document -> isVisibleOnBranch(document, input.branch()))
                .filter(document -> hasAnyTag(document, input.tags()))
                .toList();

        List<String> ids = result.stream().map(ContextDocument::id).toList();
        log.info("Selected context documents ids={} branch={} tags={}", ids, input.branch(), input.tags());
        return result;
    }

    public synchronized List<ContextDocument> updateContext(UpdateContextInput input) {
        validate(input);

        Map<String, ContextDocument> storedById = new HashMap<>();
        IContextDao.findAll().forEach(document -> storedById.put(document.id(), document));

        Set<String> requestedIds = new HashSet<>();
        List<ContextDocument> replacements = input.documents().stream()
                .map(update -> prepareReplacement(input.branch(), update, storedById, requestedIds))
                .toList();

        List<ContextDocument> result = replacements.stream()
                .map(IContextDao::write)
                .toList();

        log.info("Updated {} context documents for branch={} reason='{}'", result.size(), input.branch(), input.descriptionShort());
        return result;
    }

    private ContextDocument prepareReplacement(
            String branch,
            ContextDocument update,
            Map<String, ContextDocument> storedById,
            Set<String> requestedIds) {
        if (update == null || isBlank(update.id()) || isBlank(update.expectedVersion()) || isBlank(update.markdown())) {
            throw new RuntimeException("Every document update needs an id, expectedVersion and markdown");
        }
        if (update.tags() == null || update.tags().isEmpty()) {
            throw new RuntimeException("At least one tag is required for context document " + update.id());
        }
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
        if (!stored.version().equals(update.expectedVersion())) {
            throw new RuntimeException("Context document " + update.id() + " changed since it was read");
        }

        return new ContextDocument(stored.id(), stored.path(), update.markdown(), update.expectedVersion(), stored.version(),
                stored.scope(), stored.branch(), Set.copyOf(update.tags()));
    }

    private boolean isVisibleOnBranch(ContextDocument document, String branch) {
        return document.scope() == ContextScope.GLOBAL || branch.equals(document.branch());
    }

    private boolean hasAnyTag(ContextDocument document, Set<Tag> requestedTags) {
        return document.tags().stream().anyMatch(requestedTags::contains);
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
