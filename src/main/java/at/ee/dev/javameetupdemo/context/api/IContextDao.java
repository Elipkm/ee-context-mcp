package at.ee.dev.javameetupdemo.context.api;

import at.ee.dev.javameetupdemo.context.dto.ContextDocument;

import java.util.List;

public interface IContextDao {

    List<ContextDocument> findAll();

    ContextDocument write(ContextDocument document);
}
