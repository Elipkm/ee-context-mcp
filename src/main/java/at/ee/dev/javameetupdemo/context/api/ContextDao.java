package at.ee.dev.javameetupdemo.context.api;

import at.ee.dev.javameetupdemo.context.dto.StoredContextDocument;

import java.util.List;

public interface ContextDao {

    List<StoredContextDocument> findAll();

    StoredContextDocument write(StoredContextDocument document);
}
