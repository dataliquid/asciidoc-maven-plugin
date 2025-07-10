package com.dataliquid.maven.asciidoc.template;

import java.util.Map;

public class DocumentContext {
    private final String html;
    private final Map<String, Object> attributes;
    private final Map<String, Object> frontMatter;
    private final Map<String, Object> metadata;

    public DocumentContext(String html, Map<String, Object> attributes, 
                          Map<String, Object> frontMatter, Map<String, Object> metadata) {
        this.html = html;
        this.attributes = attributes;
        this.frontMatter = frontMatter;
        this.metadata = metadata;
    }

    public String getHtml() {
        return html;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Map<String, Object> getFrontMatter() {
        return frontMatter;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}