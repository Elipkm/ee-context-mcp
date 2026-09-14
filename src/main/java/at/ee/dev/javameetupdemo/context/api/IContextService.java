package at.ee.dev.javameetupdemo.context.api;

import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;
import at.ee.dev.javameetupdemo.mcp.GetContextInput;
import at.ee.dev.javameetupdemo.mcp.UpdateContextInput;

import java.util.List;

public interface IContextService {
    List<McpContextDocument> getContext(GetContextInput input);
    List<McpContextDocument> updateContext(UpdateContextInput input);
}
