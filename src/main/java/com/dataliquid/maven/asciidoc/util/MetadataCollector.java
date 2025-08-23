package com.dataliquid.maven.asciidoc.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dataliquid.maven.asciidoc.model.DocumentMetadata;

/**
 * Collects metadata from AsciiDoc documents during processing. Provides a
 * unified JSON structure for validation against custom schemas.
 */
public class MetadataCollector {

    private final List<DocumentMetadata> documents = new ArrayList<>();

    /**
     * Adds a document's metadata to the collection.
     *
     * @param path     Relative path to the document
     * @param metadata Combined metadata (front matter + attributes)
     */
    public void addDocument(String path, Map<String, Object> metadata) {
        documents.add(new DocumentMetadata(path, metadata));
    }

    /**
     * Converts the collection to a JSON-serializable structure.
     *
     * @return Map representing the entire collection
     */
    public Map<String, Object> toJson() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("documents", documents);
        result.put("timestamp", Instant.now().toString());
        result.put("count", documents.size());
        return result;
    }

    /**
     * Gets the number of documents in the collection.
     *
     * @return Number of documents
     */
    public int size() {
        return documents.size();
    }

    /**
     * Clears all collected documents.
     */
    public void clear() {
        documents.clear();
    }

}