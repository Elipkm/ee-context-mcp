package at.ee.dev.javameetupdemo.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskContextOrchestratorTests {

    @TempDir
    Path repository;

    @Test
    void selectsBaselineAndActiveFeatureContextWithReasons() throws IOException {
        write(".git/HEAD", "ref: refs/heads/feature/order-cancellation\n");
        write("engineering-context/index.md", """
                # Map
                - [Domain](core/domain.md) — Shared product rules.
                - [Feature](work/active/order-cancellation/overview.md) — Cancellation behavior.
                - [Other](work/active/search/overview.md) — Search behavior.
                """);
        write("engineering-context/core/domain.md", "# Domain\n");
        write("engineering-context/work/active/order-cancellation/overview.md", "# Cancellation\n");
        write("engineering-context/work/active/search/overview.md", "# Search\n");

        ContextDao dao = new FileSystemContextDao(repository.toString());
        ScopeResolver scopeResolver = new ScopeResolver(dao);
        TaskContextOrchestrator orchestrator = new TaskContextOrchestrator(
                scopeResolver, new ContextSelector(new WikiNavigator(dao), dao));

        var plan = orchestrator.prepare("Add a cancellation endpoint", repository.toString(), null);

        assertThat(plan.branch()).isEqualTo("feature/order-cancellation");
        assertThat(plan.feature()).isEqualTo("order-cancellation");
        assertThat(plan.included()).extracting(ContextModels.SelectedDocument::path)
                .containsExactly("engineering-context/core/domain.md",
                        "engineering-context/work/active/order-cancellation/overview.md");
        assertThat(plan.excluded()).extracting(ContextModels.ExcludedDocument::path)
                .containsExactly("engineering-context/work/active/search/overview.md");
    }

    @Test
    void rejectsAnotherRepository() throws IOException {
        write("engineering-context/index.md", "# Map\n");
        ContextDao dao = new FileSystemContextDao(repository.toString());
        TaskContextOrchestrator orchestrator = new TaskContextOrchestrator(
                new ScopeResolver(dao), new ContextSelector(new WikiNavigator(dao), dao));

        assertThatThrownBy(() -> orchestrator.prepare("Do work", repository.resolve("other").toString(), null))
                .isInstanceOf(ContextEngineException.class)
                .hasMessageContaining("outside the configured scope");
    }

    private void write(String relativePath, String content) throws IOException {
        Path path = repository.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }
}
