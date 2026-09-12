package at.ee.dev.javameetupdemo.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public class FileSystemContextDao implements ContextDao {

    private final Path repositoryRoot;

    public FileSystemContextDao(@Value("${context-engine.repository-root}") String repositoryRoot) {
        this.repositoryRoot = Path.of(repositoryRoot).toAbsolutePath().normalize();
    }

    @Override
    public Path repositoryRoot() {
        return repositoryRoot;
    }

    @Override
    public Optional<String> read(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(path));
        } catch (IOException exception) {
            throw new ContextEngineException("Could not read " + relativePath, exception);
        }
    }

    @Override
    public List<String> markdownFiles(String relativeDirectory) {
        Path directory = resolve(relativeDirectory);
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".md"))
                    .map(repositoryRoot::relativize)
                    .map(path -> path.toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        } catch (IOException exception) {
            throw new ContextEngineException("Could not list " + relativeDirectory, exception);
        }
    }

    @Override
    public void write(String relativePath, String content) {
        Path path = resolve(relativePath);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        } catch (IOException exception) {
            throw new ContextEngineException("Could not write " + relativePath, exception);
        }
    }

    private Path resolve(String relativePath) {
        Path resolved = repositoryRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(repositoryRoot)) {
            throw new ContextEngineException("Path leaves the configured repository: " + relativePath);
        }
        return resolved;
    }
}
