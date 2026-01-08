package com.dataliquid.maven.asciidoc.parser;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

@SuppressWarnings("PMD.GuardLogStatement")
public class FrontMatterParser {

    private final YAMLMapper yamlMapper = new YAMLMapper();
    private final JsonMapper jsonMapper = new JsonMapper();
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
                log.debug("Failed to parse front matter as YAML or JSON: " + jsonException.getMessage());
                return new HashMap<>();
            }
        }
    }

}