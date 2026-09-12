package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.ContextModels.ExcludedDocument;
import at.ee.dev.javameetupdemo.context.ContextModels.SelectedDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ContextSelector {

    private static final Set<String> ALWAYS_INCLUDED = Set.of(
            "engineering-context/core/domain.md",
            "engineering-context/core/architecture.md",
            "engineering-context/core/constraints.md",
            "engineering-context/core/tech-stack.md",
            "engineering-context/guidelines/implementation.md",
            "engineering-context/guidelines/testing.md");

    private final WikiNavigator wikiNavigator;
    private final ContextDao contextDao;

    public ContextSelector(WikiNavigator wikiNavigator, ContextDao contextDao) {
        this.wikiNavigator = wikiNavigator;
        this.contextDao = contextDao;
    }

    public Selection select(String task, ScopeResolver.Scope scope) {
        Set<String> taskTerms = terms(task + " " + scope.feature());
        List<SelectedDocument> included = new ArrayList<>();
        List<ExcludedDocument> excluded = new ArrayList<>();

        for (WikiNavigator.IndexEntry entry : wikiNavigator.entries()) {
            if (contextDao.read(entry.path()).isEmpty()) {
                excluded.add(new ExcludedDocument(entry.path(), "Listed in the context map but missing"));
                continue;
            }

            boolean requiredCore = ALWAYS_INCLUDED.contains(entry.path());
            boolean activeFeature = !scope.feature().equals("none")
                    && entry.path().startsWith("engineering-context/work/")
                    && entry.path().contains("/" + scope.feature() + "/");
            boolean taskMatch = !disjoint(taskTerms, terms(entry.path() + " " + entry.description()));

            if (requiredCore) {
                included.add(new SelectedDocument(entry.path(), "Baseline engineering context: " + entry.description()));
            } else if (activeFeature) {
                included.add(new SelectedDocument(entry.path(), "Active feature context: " + entry.description()));
            } else if (taskMatch) {
                included.add(new SelectedDocument(entry.path(), "Task keyword match: " + entry.description()));
            } else {
                excluded.add(new ExcludedDocument(entry.path(), "Outside the active feature and not relevant to the task"));
            }
        }
        return new Selection(List.copyOf(included), List.copyOf(excluded));
    }

    private boolean disjoint(Set<String> left, Set<String> right) {
        return left.stream().noneMatch(right::contains);
    }

    private Set<String> terms(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(term -> term.length() >= 4)
                .filter(term -> !Set.of("with", "from", "that", "this", "implement", "feature", "context").contains(term))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public record Selection(List<SelectedDocument> included, List<ExcludedDocument> excluded) {
    }
}
