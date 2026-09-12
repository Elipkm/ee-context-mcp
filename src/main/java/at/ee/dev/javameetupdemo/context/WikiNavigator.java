package at.ee.dev.javameetupdemo.context;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WikiNavigator {

    private static final String INDEX_PATH = "engineering-context/index.md";
    private static final Pattern LINK = Pattern.compile("^- \\[[^]]+](?:\\(([^)]+)\\))\\s*[—-]\\s*(.+)$");

    private final ContextDao contextDao;

    public WikiNavigator(ContextDao contextDao) {
        this.contextDao = contextDao;
    }

    public List<IndexEntry> entries() {
        String index = contextDao.read(INDEX_PATH)
                .orElseThrow(() -> new ContextEngineException("Missing context map: " + INDEX_PATH));
        return index.lines()
                .map(String::trim)
                .map(LINK::matcher)
                .filter(Matcher::matches)
                .map(matcher -> new IndexEntry(normalize(matcher.group(1)), matcher.group(2).trim()))
                .toList();
    }

    private String normalize(String path) {
        String normalized = path.replace('\\', '/');
        return normalized.startsWith("engineering-context/") ? normalized : "engineering-context/" + normalized;
    }

    public record IndexEntry(String path, String description) {
    }
}
