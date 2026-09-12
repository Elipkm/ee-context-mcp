package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.ContextModels.ProposedChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextUpdateManagerTests {

    @TempDir
    Path repository;

    @Test
    void proposesWithoutWritingThenAppliesAfterApproval() throws IOException {
        Path document = write("engineering-context/work/active/orders/progress.md", "# Progress\n\nOld\n");
        ContextUpdateManager manager = manager();

        var proposal = manager.propose(repository.toString(), "Document completed endpoint",
                List.of(new ProposedChange("engineering-context/work/active/orders/progress.md", "# Progress\n\nDone")));

        assertThat(Files.readString(document)).contains("Old");
        assertThat(proposal.status()).isEqualTo("PENDING_REVIEW");
        assertThat(proposal.changes().getFirst().diff()).contains("-Old", "+Done");

        var result = manager.apply(proposal.proposalId(), true);

        assertThat(result.status()).isEqualTo("APPLIED");
        assertThat(Files.readString(document)).isEqualTo("# Progress\n\nDone\n");
    }

    @Test
    void requiresApprovalAndRejectsStaleOrUnsafeChanges() throws IOException {
        Path document = write("engineering-context/work/active/orders/progress.md", "Old\n");
        ContextUpdateManager manager = manager();
        var proposal = manager.propose(null, "Update progress",
                List.of(new ProposedChange("engineering-context/work/active/orders/progress.md", "New\n")));

        assertThatThrownBy(() -> manager.apply(proposal.proposalId(), false))
                .hasMessageContaining("approval");

        Files.writeString(document, "Changed by developer\n");
        assertThatThrownBy(() -> manager.apply(proposal.proposalId(), true))
                .hasMessageContaining("changed after review");

        assertThatThrownBy(() -> manager.propose(null, "Unsafe",
                List.of(new ProposedChange("engineering-context/core/domain.md", "Replacement"))))
                .hasMessageContaining("Only Markdown below");
    }

    private ContextUpdateManager manager() {
        ContextDao dao = new FileSystemContextDao(repository.toString());
        return new ContextUpdateManager(dao, new ScopeResolver(dao));
    }

    private Path write(String relativePath, String content) throws IOException {
        Path path = repository.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }
}
