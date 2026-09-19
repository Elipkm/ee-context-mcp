package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.dto.ContextMetadata;
import at.ee.dev.javameetupdemo.context.dto.ContextRelation;
import at.ee.dev.javameetupdemo.context.enumm.ContextLoad;
import at.ee.dev.javameetupdemo.context.dto.McpContextDocument;
import at.ee.dev.javameetupdemo.context.enumm.ContextScope;
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
    void createsDocumentsInAnEmptyRepositoryAndCanReplaceThem() {
        var branchDocument = update("order-tests", "# Order tests", Tag.TEST);
        var globalDocument = new McpContextDocument("architecture", "# Architecture",
                new ContextMetadata(ContextScope.GLOBAL, Set.of(Tag.ARCHITECTURE), Set.of("component:architecture"), List.of(), null));

        var result = contextService.updateContext(new UpdateContextInput(
                "Initialize context", "feature/orders", List.of(branchDocument, globalDocument)));

        assertThat(repository.resolve("engineering-context/order-tests.md")).exists();
        assertThat(repository.resolve("engineering-context/architecture.md")).exists();
        assertThat(get("feature/orders", "use-case:orders")).isEqualTo(result.getFirst());
        assertThat(contextService.getContext(request("feature/search", "use-case:orders"))).isEmpty();
        assertThat(get("feature/search", "component:architecture")).isEqualTo(result.get(1));

        contextService.updateContext(new UpdateContextInput("Revise tests", "feature/orders",
                List.of(update("order-tests", "# Revised tests", Tag.TEST))));

        assertThat(get("feature/orders", "use-case:orders").context()).isEqualTo("# Revised tests\n");
    }

    @Test
    void createsAndReplacesDocumentsInTheSameBatch() throws IOException {
        writeDocument("existing", "BRANCH", "feature/orders", "TEST", "# Old");

        var result = contextService.updateContext(new UpdateContextInput("Save context", "feature/orders",
                List.of(update("existing", "# Updated", Tag.TEST), update("new", "# New", Tag.TEST))));

        assertThat(result).extracting(McpContextDocument::context).containsExactly("# Updated\n", "# New\n");
        assertThat(contextService.getContext(request("feature/orders", "use-case:orders")))
                .containsExactlyElementsOf(result);
    }

    @Test
    void rejectsUnsafeNewIdsBeforeWritingAnyDocument() {
        for (String invalidId : List.of("../escape", "nested/document", "UPPERCASE", "bad\nid")) {
            assertThatThrownBy(() -> contextService.updateContext(new UpdateContextInput(
                    "Invalid creation", "feature/orders", List.of(
                    update("valid", "# Valid", Tag.TEST), update(invalidId, "# Invalid", Tag.TEST)))))
                    .hasMessageContaining("id must contain only");
        }

        assertThat(repository.resolve("engineering-context")).doesNotExist();
    }

    @Test
    void rejectsDuplicateNewIdsBeforeCreatingAnyDocument() {
        assertThatThrownBy(() -> contextService.updateContext(new UpdateContextInput(
                "Duplicate creation", "feature/orders", List.of(
                update("new", "# First", Tag.TEST), update("new", "# Second", Tag.TEST)))))
                .hasMessageContaining("requested more than once");

        assertThat(repository.resolve("engineering-context")).doesNotExist();
    }

    @Test
    void returnsMatchingGlobalAndExactBranchDocumentsRegardlessOfTags() throws IOException {
        writeDocument("global-architecture", "GLOBAL", "", "ARCHITECTURE", "# Architecture");
        writeDocument("global-domain", "GLOBAL", "", "DOMAIN", "# Domain");
        writeDocument("order-tests", "BRANCH", "feature/orders", "TEST", "# Order tests");
        writeDocument("other-tests", "BRANCH", "feature/search", "TEST", "# Search tests");

        var request = new GetContextInput(
                "Implement orders",
                "Add the order endpoint and tests",
                "feature/orders",
                Set.of("use-case:orders"));

        var result = contextService.getContext(request);

        assertThat(result).extracting(McpContextDocument::id)
                .containsExactly("global-architecture", "global-domain", "order-tests");
        assertThat(result.getFirst().context()).isEqualTo("# Architecture\n");
        assertThat(result.getFirst().metadata().scope()).isEqualTo(ContextScope.GLOBAL);
    }

    @Test
    void replacesACompleteDocumentAndReturnsTheUpdatedContract() throws IOException {
        writeDocument("order-tests", "BRANCH", "feature/orders", "TEST", "# Old tests");
        var document = get("feature/orders", "use-case:orders");

        var result = contextService.updateContext(new UpdateContextInput(
                "Document the new scenarios",
                "feature/orders",
                List.of(update(document.id(), "# New tests", Tag.TEST, Tag.FEATURE))));

        var updated = get("feature/orders", "use-case:orders");
        assertThat(updated.context()).isEqualTo("# New tests\n");
        assertThat(updated.metadata().tags()).containsExactlyInAnyOrder(Tag.TEST, Tag.FEATURE);
        assertThat(result.getFirst()).isEqualTo(updated);
    }

    @Test
    void rejectsDuplicateIdsBeforeWritingAnyDocument() throws IOException {
        writeDocument("first", "BRANCH", "feature/orders", "TEST", "# First");
        writeDocument("second", "BRANCH", "feature/orders", "TEST", "# Second");
        var updates = List.of(
                update("first", "# Changed first", Tag.TEST),
                update("first", "# Changed again", Tag.TEST));

        assertThatThrownBy(() -> contextService.updateContext(
                new UpdateContextInput("Change both", "feature/orders", updates)))
                .hasMessageContaining("requested more than once");

        assertThat(get("feature/orders", "use-case:orders").context()).isEqualTo("# First\n");
    }

    @Test
    void rejectsDocumentsFromAnotherBranch() throws IOException {
        writeDocument("search", "BRANCH", "feature/search", "FEATURE", "# Search");
        var search = get("feature/search", "use-case:orders");

        assertThatThrownBy(() -> contextService.updateContext(new UpdateContextInput(
                "Wrong branch",
                "feature/orders",
                List.of(update(search.id(), "# Orders", Tag.FEATURE)))))
                .hasMessageContaining("does not belong to branch feature/orders");
    }

    @Test
    void includesBaselineAndOneHopFromSubjectMatchesWithoutLeakingOtherBranches() throws IOException {
        writeGraphDocument("architecture", "GLOBAL", "", """
                load: ALWAYS
                relations:
                  - type: REQUIRES
                    target: baseline-only-link
                """);
        writeGraphDocument("baseline-only-link", "GLOBAL", "", "");
        writeGraphDocument("booking", "BRANCH", "feature/orders", """
                subjects:
                  - use-case:import-bookings
                  - component:booking
                relations:
                  - type: REQUIRES
                    target: daily-limit
                  - type: REQUIRES
                    target: other-branch
                  - type: REQUIRES
                    target: booking
                  - type: REQUIRES
                    target: daily-limit
                """);
        writeGraphDocument("daily-limit", "GLOBAL", "", """
                relations:
                  - type: REQUIRES
                    target: second-hop
                  - type: REQUIRES
                    target: booking
                """);
        writeGraphDocument("second-hop", "GLOBAL", "", "");
        writeGraphDocument("unrelated-global", "GLOBAL", "", "subjects: [domain:payroll]\n");
        writeGraphDocument("other-branch", "BRANCH", "feature/other", """
                load: ALWAYS
                subjects: [component:booking]
                """);
        writeGraphDocument("incoming-only", "GLOBAL", "", """
                relations:
                  - type: REQUIRES
                    target: booking
                """);

        var result = contextService.getContext(request("feature/orders", "component:booking"));

        assertThat(result).extracting(McpContextDocument::id)
                .containsExactly("architecture", "booking", "daily-limit");
        assertThat(result.get(1).metadata().subjects())
                .containsExactlyInAnyOrder("use-case:import-bookings", "component:booking");
        assertThat(result.get(2).metadata().subjects()).isEmpty();
        assertThat(result.get(2).metadata().load()).isEqualTo(ContextLoad.SUBJECT_MATCH);
    }

    @Test
    void matchesAnyExactSubjectAndReturnsBaselineEvenWhenNoSubjectMatches() throws IOException {
        writeGraphDocument("architecture", "GLOBAL", "", "load: ALWAYS\nsubjects: [component:booking]\n");
        writeGraphDocument("import", "GLOBAL", "", "subjects: [use-case:import-bookings]\n");
        writeGraphDocument("unrelated", "GLOBAL", "", "subjects: [component:bookings]\n");
        writeGraphDocument("other-branch", "BRANCH", "feature/other", "subjects: [domain:other]\n");

        var result = contextService.getContext(new GetContextInput("Add import", "Import bookings",
                "feature/orders", Set.of("component:booking", "use-case:import-bookings")));
        assertThat(result).extracting(McpContextDocument::id).containsExactly("architecture", "import");
        assertThat(contextService.getContext(request("feature/orders", "domain:other")))
                .extracting(McpContextDocument::id).containsExactly("architecture");
    }

    @Test
    void persistsAllMetadataThroughCreationAndFullReplacement() {
        var initial = new ContextMetadata(ContextScope.GLOBAL, Set.of(Tag.BUSINESS_RULE),
                Set.of("component:booking", "use-case:import-bookings"),
                List.of(new ContextRelation("REQUIRES", "locking")), ContextLoad.ALWAYS);
        contextService.updateContext(new UpdateContextInput("Preserve booking consistency", "feature/orders",
                List.of(new McpContextDocument("daily-limit", "# Limit", initial),
                        new McpContextDocument("locking", "# Lock", new ContextMetadata(
                                ContextScope.GLOBAL, Set.of(Tag.ARCHITECTURE), null, null, null)))));
        var dao = new FileSystemIContextDao(repository.toString());
        assertThat(dao.findAll().getFirst().metadata()).isEqualTo(initial);
        assertThat(contextService.getContext(request("feature/orders", "component:booking")))
                .extracting(McpContextDocument::id).containsExactly("daily-limit", "locking");

        var replacement = new ContextMetadata(ContextScope.GLOBAL, Set.of(Tag.DOMAIN),
                Set.of("domain:time-tracking"), List.of(), ContextLoad.SUBJECT_MATCH);
        contextService.updateContext(new UpdateContextInput("Revise context", "feature/orders",
                List.of(new McpContextDocument("daily-limit", "# Revised limit", replacement))));
        assertThat(dao.findAll().getFirst().metadata()).isEqualTo(replacement);
        assertThatThrownBy(() -> contextService.getContext(request("feature/orders", "component:booking")))
                .hasMessageContaining("Unknown subjects");
    }

    @Test
    void rejectsMissingEmptyAndBlankRequestSubjects() {
        assertThatThrownBy(() -> contextService.getContext(new GetContextInput("Task", "Description", "main", null)))
                .hasMessageContaining("At least one subject");
        assertThatThrownBy(() -> contextService.getContext(new GetContextInput("Task", "Description", "main", Set.of())))
                .hasMessageContaining("At least one subject");
        for (String subject : List.of("", " ", "\t\n")) {
            assertThatThrownBy(() -> contextService.getContext(request("main", subject)))
                    .hasMessageContaining("non-blank");
        }
    }

    @Test
    void resolvesSynonymsToTheSameBranchScopedOneHopContextAsCanonicalSubjects() throws IOException {
        writeGlossary("""
                subjects:
                  use-case:import-bookings:
                    aliases: [booking import, timesheet upload, csv import]
                """);
        writeGraphDocument("architecture", "GLOBAL", "", "load: ALWAYS\n");
        writeGraphDocument("import", "BRANCH", "feature/orders", """
                subjects: [use-case:import-bookings]
                relations:
                  - type: REQUIRES
                    target: limit
                """);
        writeGraphDocument("limit", "GLOBAL", "", """
                relations:
                  - type: REQUIRES
                    target: second-hop
                """);
        writeGraphDocument("second-hop", "GLOBAL", "", "");
        writeGraphDocument("other-branch", "BRANCH", "feature/other", "subjects: [use-case:import-bookings]\n");

        var canonical = contextService.getContext(request("feature/orders", "use-case:import-bookings"));
        assertThat(canonical).extracting(McpContextDocument::id).containsExactly("architecture", "import", "limit");
        for (String term : List.of("booking import", "  TIMESHEET UPLOAD  ", "csv import", "USE-CASE:IMPORT-BOOKINGS")) {
            assertThat(contextService.getContext(request("feature/orders", term))).isEqualTo(canonical);
        }
        assertThat(contextService.getContext(new GetContextInput("Import", "Import bookings", "feature/orders",
                Set.of("csv import", "timesheet upload", "use-case:import-bookings")))).isEqualTo(canonical);
    }

    @Test
    void reportsUnknownTermsWithAvailableAliasesInsteadOfReturningPartialOrBaselineOnlyResults() throws IOException {
        writeGlossary("""
                subjects:
                  use-case:import-bookings:
                    aliases: [booking import, timesheet upload]
                """);
        writeGraphDocument("architecture", "GLOBAL", "", "load: ALWAYS\n");
        writeGraphDocument("import", "GLOBAL", "", "subjects: [use-case:import-bookings]\n");

        for (Set<String> terms : List.of(Set.of("unknown upload"), Set.of("booking import", "unknown upload"))) {
            assertThatThrownBy(() -> contextService.getContext(new GetContextInput("Task", "Description", "main", terms)))
                    .hasMessageContaining("Unknown subjects: [unknown upload]")
                    .hasMessageContaining("Available subjects")
                    .hasMessageContaining("use-case:import-bookings")
                    .hasMessageContaining("timesheet upload");
        }
    }

    @Test
    void rejectsAmbiguousAliasesAfterNormalizationEvenForACanonicalRequest() throws IOException {
        writeGlossary("""
                subjects:
                  use-case:import-bookings:
                    aliases: [upload]
                  use-case:import-payroll:
                    aliases: [' UPLOAD ']
                """);

        assertThatThrownBy(() -> contextService.getContext(request("main", "use-case:import-bookings")))
                .hasMessageContaining("Ambiguous subject alias")
                .hasMessageContaining("use-case:import-bookings")
                .hasMessageContaining("use-case:import-payroll");
    }

    @Test
    void rejectsAliasesThatShadowAnotherCanonicalSubjectInDocuments() throws IOException {
        writeGraphDocument("booking", "GLOBAL", "", "subjects: [component:booking]\n");
        writeGlossary("""
                subjects:
                  component:payroll:
                    aliases: [component:booking]
                """);

        assertThatThrownBy(() -> contextService.getContext(request("main", "component:booking")))
                .hasMessageContaining("Ambiguous subject alias");
    }

    @Test
    void acceptsDuplicateAliasesForTheSameSubjectAndReloadsGlossaryChanges() throws IOException {
        writeGraphDocument("booking", "GLOBAL", "", "subjects: [component:booking]\n");
        writeGlossary("""
                subjects:
                  component:booking:
                    aliases: [booking, ' BOOKING ', component:booking]
                """);
        assertThat(contextService.getContext(request("main", "booking")))
                .extracting(McpContextDocument::id).containsExactly("booking");
        writeGlossary("""
                subjects:
                  component:booking:
                    aliases: [time entries]
                """);
        assertThat(contextService.getContext(request("main", "time entries")))
                .extracting(McpContextDocument::id).containsExactly("booking");
        assertThatThrownBy(() -> contextService.getContext(request("main", "booking")))
                .hasMessageContaining("Unknown subjects");
    }

    @Test
    void supportsCanonicalSubjectsWithoutAGlossary() throws IOException {
        writeGraphDocument("booking", "GLOBAL", "", "subjects: [component:booking]\n");
        assertThat(new FileSystemIContextDao(repository.toString()).readSubjectAliases()).isEmpty();
        assertThat(contextService.getContext(request("main", "component:booking")))
                .extracting(McpContextDocument::id).containsExactly("booking");
        assertThatThrownBy(() -> contextService.getContext(request("main", "booking")))
                .hasMessageContaining("Unknown subjects").hasMessageContaining("component:booking");
    }

    @Test
    void rejectsMalformedGlossaries() throws IOException {
        for (String yaml : List.of("subjects: []", "subjects: {booking: {aliases: [booking]}}",
                "subjects: {component:booking: {aliases: booking}}",
                "subjects: {component:booking: {aliases: [' ']}}",
                "subjects: {component:booking: {aliases: [123]}}",
                "subjects:\n  component:booking:\n    aliases: [booking]\n  component:booking:\n    aliases: [entries]\n")) {
            writeGlossary(yaml);
            assertThatThrownBy(() -> new FileSystemIContextDao(repository.toString()).readSubjectAliases())
                    .isInstanceOf(RuntimeException.class);
        }
    }

    private void writeGlossary(String yaml) throws IOException {
        Path path = repository.resolve("engineering-context/subjects.yaml");
        Files.createDirectories(path.getParent());
        Files.writeString(path, yaml);
    }

    @Test
    void reportsMissingRelationTargets() throws IOException {
        writeGraphDocument("booking", "GLOBAL", "", """
                subjects: [component:booking]
                relations:
                  - type: REQUIRES
                    target: missing
                """);
        assertThatThrownBy(() -> contextService.getContext(request("main", "component:booking")))
                .hasMessageContaining("booking references missing document missing");
    }

    @Test
    void rejectsMalformedGraphMetadata() throws IOException {
        for (String metadata : List.of("subjects: component:booking\n", "subjects: [booking]\n",
                "relations: [locking]\n", "load: EVERYTHING\n", "relations: [{type: REQUIRES}]\n")) {
            writeGraphDocument("invalid", "GLOBAL", "", metadata);
            assertThatThrownBy(() -> new FileSystemIContextDao(repository.toString()).findAll())
                    .isInstanceOf(RuntimeException.class);
        }
    }

    private void writeGraphDocument(String id, String scope, String branch, String extra) throws IOException {
        Path path = repository.resolve("engineering-context/" + id + ".md");
        Files.createDirectories(path.getParent());
        Files.writeString(path, """
                ---
                id: %s
                scope: %s
                branch: %s
                tags: [BUSINESS_RULE]
                %s---
                # %s
                """.formatted(id, scope, branch, extra, id));
    }

    private McpContextDocument get(String branch, String subject) {
        return contextService.getContext(request(branch, subject)).getFirst();
    }

    private McpContextDocument update(String id, String context, Tag... tags) {
        return new McpContextDocument(id, context, new ContextMetadata(ContextScope.BRANCH, Set.of(tags), Set.of("use-case:orders"), List.of(), null));
    }

    private GetContextInput request(String branch, String subject) {
        return new GetContextInput("Test task", "Test description", branch, Set.of(subject));
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
                subjects: [use-case:orders]
                ---
                %s
                """.formatted(id, scope, branch, tags, markdown));
    }
}
