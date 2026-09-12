package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.ContextModels.ContextPlan;
import org.springframework.stereotype.Service;

@Service
public class TaskContextOrchestrator {

    private final ScopeResolver scopeResolver;
    private final ContextSelector contextSelector;

    public TaskContextOrchestrator(ScopeResolver scopeResolver, ContextSelector contextSelector) {
        this.scopeResolver = scopeResolver;
        this.contextSelector = contextSelector;
    }

    public ContextPlan prepare(String task, String repository, String feature) {
        if (task == null || task.isBlank()) {
            throw new ContextEngineException("Task must not be blank");
        }
        ScopeResolver.Scope scope = scopeResolver.resolve(repository, feature);
        ContextSelector.Selection selection = contextSelector.select(task, scope);
        return new ContextPlan(
                task.trim(),
                scope.repository(),
                scope.branch(),
                scope.feature(),
                selection.included(),
                selection.excluded(),
                "Read each included repository-relative Markdown path once before changing code. "
                        + "After implementation and tests, propose any needed living-context updates for developer review.");
    }
}
