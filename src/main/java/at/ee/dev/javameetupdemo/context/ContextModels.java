package at.ee.dev.javameetupdemo.context;

import java.util.List;

public final class ContextModels {

    private ContextModels() {
    }

    public record SelectedDocument(String path, String reason) {
    }

    public record ExcludedDocument(String path, String reason) {
    }

    public record ContextPlan(
            String task,
            String repository,
            String branch,
            String feature,
            List<SelectedDocument> included,
            List<ExcludedDocument> excluded,
            String instruction) {
    }

    public record ProposedChange(String path, String content) {
    }

    public record FileDiff(String path, String operation, String diff) {
    }

    public record UpdateProposal(
            String proposalId,
            String summary,
            List<FileDiff> changes,
            String status,
            String instruction) {
    }

    public record ApplyResult(String proposalId, List<String> updatedPaths, String status) {
    }
}
