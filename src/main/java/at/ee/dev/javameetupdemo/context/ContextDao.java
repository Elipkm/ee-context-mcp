package at.ee.dev.javameetupdemo.context;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface ContextDao {

    Path repositoryRoot();

    Optional<String> read(String relativePath);

    List<String> markdownFiles(String relativeDirectory);

    void write(String relativePath, String content);
}
