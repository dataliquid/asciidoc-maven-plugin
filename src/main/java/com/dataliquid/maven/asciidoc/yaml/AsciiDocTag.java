package com.dataliquid.maven.asciidoc.yaml;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;

/**
 * Custom YAML constructor for handling !asciidoc tags
 */
public class AsciiDocTag extends Constructor {

    public static final Tag ASCIIDOC_TAG = new Tag("!asciidoc");

    public AsciiDocTag() {
        super(new LoaderOptions());
        this.yamlConstructors.put(ASCIIDOC_TAG, new ConstructAsciiDoc());
    }

    private class ConstructAsciiDoc extends AbstractConstruct {
        @Override
        public Object construct(Node node) {
            ScalarNode scalarNode = (ScalarNode) node;
            String value = scalarNode.getValue();
            // Wrap the content in a marker object so we can identify it during traversal
            return new AsciiDocContent(value);
        }
    }

    /**
     * Marker class to identify AsciiDoc content that needs processing
     */
    public static class AsciiDocContent {
        private final String content;
        private String rendered;

        public AsciiDocContent(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }

        public String getRendered() {
            return rendered;
        }

        public void setRendered(String rendered) {
            this.rendered = rendered;
        }

        @Override
        public String toString() {
            return rendered != null ? rendered : content;
        }
    }
}