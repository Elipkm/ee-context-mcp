package at.ee.dev.javameetupdemo.context;

import at.ee.dev.javameetupdemo.context.ContextModels.StoredContextDocument;

import java.util.List;

public interface ContextDao {

    List<StoredContextDocument> findAll();

    StoredContextDocument write(StoredContextDocument document);
}
