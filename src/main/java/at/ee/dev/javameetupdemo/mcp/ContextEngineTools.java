package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;
import at.ee.dev.javameetupdemo.context.api.IContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextEngineTools {

    private static final Logger log = LoggerFactory.getLogger(ContextEngineTools.class);

    private final IContextService contextService;

    public ContextEngineTools(IContextService contextService) {
        this.contextService = contextService;
    }

    @McpTool(
            name = "get_context",
            description = "Get context documents relevant to a task. Global and exact-branch documents are matched by any requested tag.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<McpContextDocument> getContext(
            @McpToolParam(description = "Task, exact Git branch and context tags selected by the coding agent", required = true)
            GetContextInput input) {
        log.info("MCP get_context request received");
        return contextService.getContext(input);
    }

    @McpTool(
            name = "update_context",
            description = """
                    Create or fully replace minimal, developer-approved engineering context. Explicit developer corrections and reusable instructions about how code must, must not or should be written are high-priority context candidates, even when the code now demonstrates the preferred pattern. Also store new or changed durable decisions, business rules, constraints, rationale or unresolved risks that will materially help future work. Do not store task summaries, test results, transient status, repository inventories, setup details or information already documented elsewhere. Prefer updating the canonical existing document. No call is needed when nothing qualifies. Every submitted document must contain its complete context and metadata; new ids may contain only lowercase letters, numbers and hyphens.
                    """,
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false))
    public List<McpContextDocument> updateContext(
            @McpToolParam(description = "Developer-approved minimal update: exact Git branch, concise reason for the durable knowledge change, and only the complete documents that must be created or replaced", required = true)
            UpdateContextInput input) {
        log.info("MCP update_context request received");
        return contextService.updateContext(input);
    }
}
