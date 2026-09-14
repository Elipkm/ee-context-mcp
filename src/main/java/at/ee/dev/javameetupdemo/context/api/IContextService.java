package at.ee.dev.javameetupdemo.context.api;

import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.mcp.GetContextInput;
import at.ee.dev.javameetupdemo.mcp.UpdateContextInput;

import java.util.List;

public interface IContextService {
    List<ContextDocument> getContext(GetContextInput input);
    List<ContextDocument> updateContext(UpdateContextInput input);
}
