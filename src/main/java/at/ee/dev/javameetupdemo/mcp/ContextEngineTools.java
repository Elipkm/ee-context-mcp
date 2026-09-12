package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.ContextModels.ApplyResult;
import at.ee.dev.javameetupdemo.context.ContextModels.ContextPlan;
import at.ee.dev.javameetupdemo.context.ContextModels.ProposedChange;
import at.ee.dev.javameetupdemo.context.ContextModels.UpdateProposal;
import at.ee.dev.javameetupdemo.context.ContextUpdateManager;
import at.ee.dev.javameetupdemo.context.TaskContextOrchestrator;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextEngineTools {

    private final TaskContextOrchestrator orchestrator;
    private final ContextUpdateManager updateManager;

    public ContextEngineTools(TaskContextOrchestrator orchestrator, ContextUpdateManager updateManager) {
        this.orchestrator = orchestrator;
        this.updateManager = updateManager;
    }

    @McpTool(
            name = "prepare_task_context",
            description = "Select repository-local engineering context for a coding task. Returns paths and reasons, not file contents.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public ContextPlan prepareTaskContext(
            @McpToolParam(description = "The coding task to implement", required = true) String task,
            @McpToolParam(description = "Absolute repository path; omit to use the configured repository", required = false) String repository,
            @McpToolParam(description = "Active feature name; omit to infer it from the Git branch", required = false) String feature) {
        return orchestrator.prepare(task, repository, feature);
    }

    @McpTool(
            name = "propose_context_updates",
            description = "Validate living-context changes and return a reviewable diff. This does not write files.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = false, openWorldHint = false))
    public UpdateProposal proposeContextUpdates(
            @McpToolParam(description = "Absolute repository path; omit to use the configured repository", required = false) String repository,
            @McpToolParam(description = "Short summary connecting the implementation to its context changes", required = true) String summary,
            @McpToolParam(description = "Complete replacement content for Markdown files below engineering-context/work", required = true)
            List<ProposedChange> changes) {
        return updateManager.propose(repository, summary, changes);
    }

    @McpTool(
            name = "apply_context_updates",
            description = "Apply a previously reviewed proposal. Set approved=true only after explicit developer approval.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false))
    public ApplyResult applyContextUpdates(
            @McpToolParam(description = "Proposal ID returned by propose_context_updates", required = true) String proposalId,
            @McpToolParam(description = "Must be true only after the developer explicitly approved the displayed diff", required = true) boolean approved) {
        return updateManager.apply(proposalId, approved);
    }
}
