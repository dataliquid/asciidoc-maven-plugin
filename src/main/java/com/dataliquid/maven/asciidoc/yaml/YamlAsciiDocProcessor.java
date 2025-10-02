package com.dataliquid.maven.asciidoc.yaml;

import org.apache.maven.plugin.logging.Log;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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