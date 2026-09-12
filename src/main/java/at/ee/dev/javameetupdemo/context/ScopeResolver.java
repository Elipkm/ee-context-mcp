package at.ee.dev.javameetupdemo.context;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class ScopeResolver {

    private final ContextDao contextDao;

    public ScopeResolver(ContextDao contextDao) {
        this.contextDao = contextDao;
    }

    public Scope resolve(String requestedRepository, String requestedFeature) {
        Path configured = contextDao.repositoryRoot();
        Path requested = requestedRepository == null || requestedRepository.isBlank()
                ? configured
                : Path.of(requestedRepository).toAbsolutePath().normalize();
        if (!requested.equals(configured)) {
            throw new ContextEngineException("Repository is outside the configured scope: " + requested);
        }

        String branch = readBranch(configured);
        String feature = hasText(requestedFeature) ? slug(requestedFeature) : featureFrom(branch);
        return new Scope(configured.toString(), branch, feature);
    }

    private String readBranch(Path repository) {
        Path dotGit = repository.resolve(".git");
        Path head = Files.isDirectory(dotGit) ? dotGit.resolve("HEAD") : resolveWorktreeHead(dotGit);
        try {
            if (!Files.isRegularFile(head)) {
                return "unknown";
            }
            String value = Files.readString(head).trim();
            return value.startsWith("ref: refs/heads/") ? value.substring("ref: refs/heads/".length()) : "detached";
        } catch (Exception exception) {
            return "unknown";
        }
    }

    private Path resolveWorktreeHead(Path dotGit) {
        try {
            String pointer = Files.readString(dotGit).trim();
            if (!pointer.startsWith("gitdir: ")) {
                return dotGit.resolve("HEAD");
            }
            Path gitDirectory = Path.of(pointer.substring(8));
            if (!gitDirectory.isAbsolute()) {
                gitDirectory = dotGit.getParent().resolve(gitDirectory).normalize();
            }
            return gitDirectory.resolve("HEAD");
        } catch (Exception exception) {
            return dotGit.resolve("HEAD");
        }
    }

    private String featureFrom(String branch) {
        if (branch.equals("unknown") || branch.equals("detached") || branch.equals("main") || branch.equals("master")) {
            return "none";
        }
        int slash = branch.lastIndexOf('/');
        return slug(slash >= 0 ? branch.substring(slash + 1) : branch);
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Scope(String repository, String branch, String feature) {
    }
}
