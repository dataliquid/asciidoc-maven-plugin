package com.dataliquid.maven.asciidoc.yaml;

import org.apache.maven.plugin.logging.Log;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Processes YAML files containing !asciidoc tags
 */
public class YamlAsciiDocProcessor {

    private final Asciidoctor asciidoctor;
    private final Log log;
    private final Options asciidoctorOptions;

    public YamlAsciiDocProcessor(Asciidoctor asciidoctor, Options asciidoctorOptions, Log log) {
        this.asciidoctor = asciidoctor;
        this.asciidoctorOptions = asciidoctorOptions;
        this.log = log;
    }

    /**
     * Container for extracted AsciiDoc content with its YAML path context
     */
    public static class ExtractedContent {
        private final String content;
        private final String yamlPath;

        public ExtractedContent(String content, String yamlPath) {
            this.content = content;
            this.yamlPath = yamlPath;
        }

        public String getContent() {
            return content;
        }

        public String getYamlPath() {
            return yamlPath;
        }
    }

    /**
     * Extract AsciiDoc content from YAML file without rendering Used for linting
     * purposes
     *
     * @param  yamlFile the YAML file to process
     *
     * @return          list of extracted AsciiDoc content with their YAML paths
     */
    public List<ExtractedContent> extractAsciiDocContent(Path yamlFile) throws IOException {
        String content = Files.readString(yamlFile);

        // Parse YAML with custom constructor
        AsciiDocTag constructor = new AsciiDocTag();
        Yaml yaml = new Yaml(constructor);
        Object data = yaml.load(content);

        // Traverse and extract AsciiDoc content with paths
        List<ExtractedContent> extracted = new ArrayList<>();
        traverseAndExtract(data, "", extracted);
        return extracted;
    }

    /**
     * Recursively traverse the YAML structure and extract AsciiDoc content with
     * paths
     */
    @SuppressWarnings("unchecked")
    private void traverseAndExtract(Object node, String currentPath, List<ExtractedContent> results) {
        if (node instanceof AsciiDocTag.AsciiDocContent) {
            AsciiDocTag.AsciiDocContent asciiDocContent = (AsciiDocTag.AsciiDocContent) node;
            results.add(new ExtractedContent(asciiDocContent.getContent(), currentPath));
            log.debug("Extracted AsciiDoc content at path: " + currentPath);
        } else if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String newPath = currentPath.isEmpty() ? key : currentPath + "." + key;

                if (value instanceof AsciiDocTag.AsciiDocContent) {
                    AsciiDocTag.AsciiDocContent asciiDocContent = (AsciiDocTag.AsciiDocContent) value;
                    results.add(new ExtractedContent(asciiDocContent.getContent(), newPath));
                    log.debug("Extracted AsciiDoc content at path: " + newPath);
                } else {
                    traverseAndExtract(value, newPath, results);
                }
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                String newPath = currentPath + "[" + i + "]";

                if (item instanceof AsciiDocTag.AsciiDocContent) {
                    AsciiDocTag.AsciiDocContent asciiDocContent = (AsciiDocTag.AsciiDocContent) item;
                    results.add(new ExtractedContent(asciiDocContent.getContent(), newPath));
                    log.debug("Extracted AsciiDoc content at path: " + newPath);
                } else {
                    traverseAndExtract(item, newPath, results);
                }
            }
        }
    }

    /**
     * Process a YAML file and render any !asciidoc content
     */
    public String processYamlFile(Path yamlFile) throws IOException {
        String content = Files.readString(yamlFile);

        // Parse YAML with custom constructor
        AsciiDocTag constructor = new AsciiDocTag();
        Yaml yaml = new Yaml(constructor);
        Object data = yaml.load(content);

        // Traverse and render AsciiDoc content
        traverseAndRender(data);

        // Convert back to YAML/HTML
        return convertToOutput(data);
    }

    /**
     * Recursively traverse the YAML structure and render AsciiDoc content
     */
    @SuppressWarnings("unchecked")
    private void traverseAndRender(Object node) {
        if (node instanceof AsciiDocTag.AsciiDocContent) {
            AsciiDocTag.AsciiDocContent asciiDocContent = (AsciiDocTag.AsciiDocContent) node;
            String rendered = renderAsciiDoc(asciiDocContent.getContent());
            asciiDocContent.setRendered(rendered);
            log.debug("Rendered AsciiDoc content: " + rendered);
        } else if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof AsciiDocTag.AsciiDocContent) {
                    AsciiDocTag.AsciiDocContent asciiDocContent = (AsciiDocTag.AsciiDocContent) value;
                    String rendered = renderAsciiDoc(asciiDocContent.getContent());
                    asciiDocContent.setRendered(rendered);
                    // Replace the AsciiDocContent with rendered HTML
                    entry.setValue(rendered);
                } else {
                    traverseAndRender(value);
                }
            }
        } else if (node instanceof List) {
            List<Object> list = (List<Object>) node;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof AsciiDocTag.AsciiDocContent) {
                    AsciiDocTag.AsciiDocContent asciiDocContent = (AsciiDocTag.AsciiDocContent) item;
                    String rendered = renderAsciiDoc(asciiDocContent.getContent());
                    asciiDocContent.setRendered(rendered);
                    // Replace the AsciiDocContent with rendered HTML
                    list.set(i, rendered);
                } else {
                    traverseAndRender(item);
                }
            }
        }
    }

    /**
     * Render AsciiDoc content to HTML using the provided Asciidoctor instance
     */
    private String renderAsciiDoc(String asciiDocContent) {
        try {
            log.debug("Rendering AsciiDoc content: " + asciiDocContent);
            String rendered = asciidoctor.convert(asciiDocContent, asciidoctorOptions);
            return rendered != null ? rendered : "";
        } catch (Exception e) {
            log.error("Failed to render AsciiDoc content", e);
            return "<!-- Error rendering AsciiDoc: " + e.getMessage() + " -->";
        }
    }

    /**
     * Convert the processed data structure back to YAML format
     */
    private String convertToOutput(Object data) {
        // Output as YAML with rendered AsciiDoc content
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setWidth(Integer.MAX_VALUE); // Prevent line wrapping

        Yaml yaml = new Yaml(options);
        return yaml.dump(data);
    }
}