package at.ee.dev.javameetupdemo.mcp;

import at.ee.dev.javameetupdemo.context.ContextModels.ContextDocument;
import at.ee.dev.javameetupdemo.context.ContextModels.GetContextInput;
import at.ee.dev.javameetupdemo.context.ContextModels.UpdatedDocument;
import at.ee.dev.javameetupdemo.context.ContextModels.UpdateContextInput;
import at.ee.dev.javameetupdemo.context.ContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContextEngineTools {

    private static final Logger log = LoggerFactory.getLogger(ContextEngineTools.class);

    private final ContextService contextService;

    public ContextEngineTools(ContextService contextService) {
        this.contextService = contextService;
    }

    @McpTool(
            name = "get_context",
            description = "Get context documents relevant to a task. Global and exact-branch documents are matched by any requested tag.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false, idempotentHint = true, openWorldHint = false))
    public List<ContextDocument> getContext(
            @McpToolParam(description = "Task, exact Git branch and context tags selected by the coding agent", required = true)
            GetContextInput input) {
        log.info("MCP get_context request received");
        return contextService.getContext(input);
    }

    @McpTool(
            name = "update_context",
            description = "Fully replace existing context documents by id. Every replacement must contain the version returned by get_context.",
            generateOutputSchema = true,
            annotations = @McpTool.McpAnnotations(readOnlyHint = false, destructiveHint = true, idempotentHint = false, openWorldHint = false))
    public List<UpdatedDocument> updateContext(
            @McpToolParam(description = "Exact Git branch, reason and version-protected full document replacements", required = true)
            UpdateContextInput input) {
        log.info("MCP update_context request received");
        return contextService.updateContext(input);
    }
}
