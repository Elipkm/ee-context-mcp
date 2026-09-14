package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.dto.ContextDocument;
import at.ee.dev.javameetupdemo.context.impl.ContextService;
import at.ee.dev.javameetupdemo.context.impl.FileSystemIContextDao;
import at.ee.dev.javameetupdemo.mcp.GetContextInput;
import at.ee.dev.javameetupdemo.context.enumm.Tag;
import at.ee.dev.javameetupdemo.mcp.UpdateContextInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextServiceTests {

    @TempDir
    Path repository;

    private ContextService contextService;

    @BeforeEach
    void setUp() {
        contextService = new ContextService(new FileSystemIContextDao(repository.toString()));
    }

    @Test
    void returnsMatchingGlobalAndExactBranchDocumentsUsingOrTags() throws IOException {
        writeDocument("global-architecture", "GLOBAL", "", "ARCHITECTURE", "# Architecture");
        writeDocument("global-domain", "GLOBAL", "", "DOMAIN", "# Domain");
        writeDocument("order-tests", "BRANCH", "feature/orders", "TEST", "# Order tests");
        writeDocument("other-tests", "BRANCH", "feature/search", "TEST", "# Search tests");

        var request = new GetContextInput(
                "Implement orders",
                "Add the order endpoint and tests",
                "feature/orders",
                Set.of(Tag.ARCHITECTURE, Tag.TEST));

        var result = contextService.getContext(request);

        assertThat(result).extracting(ContextDocument::id)
                .containsExactly("global-architecture", "order-tests");
        assertThat(result.getFirst().markdown()).isEqualTo("# Architecture\n");
        assertThat(result.getFirst().version()).hasSize(64);
    }

    @Test
    void replacesACompleteDocumentAndReturnsItsNewVersion() throws IOException {
        writeDocument("order-tests", "BRANCH", "feature/orders", "TEST", "# Old tests");
        var document = get("feature/orders", Tag.TEST);

        var result = contextService.updateContext(new UpdateContextInput(
                "Document the new scenarios",
                "feature/orders",
                List.of(ContextDocument.update(
                        document.id(), document.version(), "# New tests", Set.of(Tag.TEST, Tag.FEATURE)))));

        var updated = get("feature/orders", Tag.FEATURE);
        assertThat(updated.markdown()).isEqualTo("# New tests\n");
        assertThat(updated.tags()).containsExactlyInAnyOrder(Tag.TEST, Tag.FEATURE);
        assertThat(result.getFirst().version()).isEqualTo(updated.version()).isNotEqualTo(document.version());
    }

    @Test
    void rejectsStaleVersionsBeforeWritingAnyDocument() throws IOException {
        writeDocument("first", "BRANCH", "feature/orders", "TEST", "# First");
        writeDocument("second", "BRANCH", "feature/orders", "TEST", "# Second");
        var documents = contextService.getContext(request("feature/orders", Tag.TEST));

        var updates = List.of(
                ContextDocument.update("first", documents.get(0).version(), "# Changed first", Set.of(Tag.TEST)),
                ContextDocument.update("second", "stale-version", "# Changed second", Set.of(Tag.TEST)));

        assertThatThrownBy(() -> contextService.updateContext(
                new UpdateContextInput("Change both", "feature/orders", updates)))
                .hasMessageContaining("changed since it was read");

        assertThat(get("feature/orders", Tag.TEST).markdown()).isEqualTo("# First\n");
    }

    @Test
    void rejectsDocumentsFromAnotherBranch() throws IOException {
        writeDocument("search", "BRANCH", "feature/search", "FEATURE", "# Search");
        var search = get("feature/search", Tag.FEATURE);

        assertThatThrownBy(() -> contextService.updateContext(new UpdateContextInput(
                "Wrong branch",
                "feature/orders",
                List.of(ContextDocument.update(search.id(), search.version(), "# Orders", Set.of(Tag.FEATURE))))))
                .hasMessageContaining("does not belong to branch feature/orders");
    }

    private ContextDocument get(String branch, Tag tag) {
        return contextService.getContext(request(branch, tag)).getFirst();
    }

    private GetContextInput request(String branch, Tag tag) {
        return new GetContextInput("Test task", "Test description", branch, Set.of(tag));
    }

    private void writeDocument(String id, String scope, String branch, String tags, String markdown) throws IOException {
        Path path = repository.resolve("engineering-context/" + id + ".md");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                ---
                id: %s
                scope: %s
                branch: %s
                tags: [%s]
                ---
                %s
                """.formatted(id, scope, branch, tags, markdown));
    }
}
