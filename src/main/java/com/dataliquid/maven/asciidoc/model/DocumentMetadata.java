package com.dataliquid.maven.asciidoc.model;

import java.util.HashMap;
import java.util.Map;

public class DocumentMetadata {
    private final String path;
    private final Map<String, Object> metadata;
    
    public DocumentMetadata(String path, Map<String, Object> metadata) {
        this.path = path;
        this.metadata = new HashMap<>(metadata);
    }
    
    public String getPath() {
        return path;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}