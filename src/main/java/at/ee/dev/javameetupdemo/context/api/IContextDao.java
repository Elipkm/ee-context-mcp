package at.ee.dev.javameetupdemo.context.api;

import at.ee.dev.javameetupdemo.context.dto.ContextDocument;

import java.util.List;
import java.util.Map;

public interface IContextDao {

    List<ContextDocument> findAll();

    Map<String, List<String>> readSubjectAliases();

    ContextDocument write(ContextDocument document);
}
