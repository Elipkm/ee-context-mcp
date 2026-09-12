package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.ContextModels.ApplyResult;
import at.ee.dev.javameetupdemo.context.ContextModels.FileDiff;
import at.ee.dev.javameetupdemo.context.ContextModels.ProposedChange;
import at.ee.dev.javameetupdemo.context.ContextModels.UpdateProposal;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContextUpdateManager {

    private static final String WRITABLE_PREFIX = "engineering-context/work/";
    private final ContextDao contextDao;
    private final ScopeResolver scopeResolver;
    private final Map<String, PendingProposal> proposals = new ConcurrentHashMap<>();

    public ContextUpdateManager(ContextDao contextDao, ScopeResolver scopeResolver) {
        this.contextDao = contextDao;
        this.scopeResolver = scopeResolver;
    }

    public UpdateProposal propose(String repository, String summary, List<ProposedChange> changes) {
        scopeResolver.resolve(repository, null);
        if (summary == null || summary.isBlank()) {
            throw new ContextEngineException("Change summary must not be blank");
        }
        if (changes == null || changes.isEmpty()) {
            throw new ContextEngineException("At least one context change is required");
        }

        List<PendingChange> pendingChanges = new ArrayList<>();
        List<FileDiff> diffs = new ArrayList<>();
        for (ProposedChange change : changes) {
            validate(change);
            String path = normalize(change.path());
            String before = contextDao.read(path).orElse("");
            String after = ensureTrailingNewline(change.content());
            pendingChanges.add(new PendingChange(path, after, hash(before)));
            diffs.add(new FileDiff(path, before.isEmpty() ? "CREATE" : "UPDATE", diff(path, before, after)));
        }

        String id = UUID.randomUUID().toString();
        proposals.put(id, new PendingProposal(summary.trim(), List.copyOf(pendingChanges)));
        return new UpdateProposal(id, summary.trim(), List.copyOf(diffs), "PENDING_REVIEW",
                "Show this diff to the developer. Call apply_context_updates only after explicit approval.");
    }

    public ApplyResult apply(String proposalId, boolean approved) {
        if (!approved) {
            throw new ContextEngineException("Explicit developer approval is required");
        }
        PendingProposal proposal = proposals.get(proposalId);
        if (proposal == null) {
            throw new ContextEngineException("Unknown or already applied proposal: " + proposalId);
        }

        for (PendingChange change : proposal.changes()) {
            String current = contextDao.read(change.path()).orElse("");
            if (!hash(current).equals(change.originalHash())) {
                throw new ContextEngineException("Context changed after review; create a new proposal for " + change.path());
            }
        }
        proposal.changes().forEach(change -> contextDao.write(change.path(), change.content()));
        proposals.remove(proposalId);
        return new ApplyResult(proposalId, proposal.changes().stream().map(PendingChange::path).toList(), "APPLIED");
    }

    private void validate(ProposedChange change) {
        if (change == null || change.path() == null || change.content() == null) {
            throw new ContextEngineException("Every proposed change needs a path and content");
        }
        String path = normalize(change.path());
        if (!path.startsWith(WRITABLE_PREFIX) || !path.endsWith(".md") || path.contains("..")) {
            throw new ContextEngineException("Only Markdown below " + WRITABLE_PREFIX + " can be updated: " + path);
        }
        if (change.content().isBlank()) {
            throw new ContextEngineException("Context content must not be blank: " + path);
        }
    }

    private String normalize(String path) {
        return path.replace('\\', '/').replaceFirst("^/+", "");
    }

    private String ensureTrailingNewline(String content) {
        return content.endsWith("\n") ? content : content + "\n";
    }

    private String diff(String path, String before, String after) {
        StringBuilder result = new StringBuilder("--- a/").append(path).append('\n')
                .append("+++ b/").append(path).append('\n');
        if (!before.isEmpty()) {
            before.lines().forEach(line -> result.append('-').append(line).append('\n'));
        }
        after.lines().forEach(line -> result.append('+').append(line).append('\n'));
        return result.toString();
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private record PendingChange(String path, String content, String originalHash) {
    }

    private record PendingProposal(String summary, List<PendingChange> changes) {
    }
}
