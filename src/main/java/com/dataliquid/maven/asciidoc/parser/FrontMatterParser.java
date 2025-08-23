package com.dataliquid.maven.asciidoc.parser;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class FrontMatterParser {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Log log;

    public FrontMatterParser() {
        this.log = new SystemStreamLog();
    }

    public FrontMatterParser(Log log) {
        this.log = log;
    }

    public Map<String, Object> parse(String content) {
        try {
            // Jackson's YAML parser can handle both YAML and JSON
            // Try YAML first since it's more permissive
            return yamlMapper.readValue(content, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception yamlException) {
            try {
                // If YAML parsing fails, try JSON
                return jsonMapper.readValue(content, new TypeReference<Map<String, Object>>() {
                });
            } catch (Exception jsonException) {
                if (log.isDebugEnabled()) {
                    log.debug("Failed to parse front matter as YAML or JSON: " + jsonException.getMessage());
                }
                return new HashMap<>();
            }
        }
    }

}