package com.dataliquid.maven.asciidoc.yaml;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Custom YAML constructor for handling !asciidoc tags
 */
public class AsciiDocTag extends Constructor {

    public static final Tag ASCIIDOC_TAG = new Tag("!asciidoc");

    public AsciiDocTag() {
        super(new LoaderOptions());
        this.yamlConstructors.put(ASCIIDOC_TAG, new ConstructAsciiDoc());
        // Always register fallback constructor for unknown tags
        this.yamlMultiConstructors.put("", new ConstructUnknownTag());
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
     * Fallback constructor for unknown tags when preserveUnknownTags is enabled.
     * Wraps unknown tags in TaggedValue to preserve tag information.
     */
    private class ConstructUnknownTag extends AbstractConstruct {
        @Override
        public Object construct(Node node) {
            if (node instanceof ScalarNode) {
                ScalarNode scalarNode = (ScalarNode) node;
                String tag = scalarNode.getTag().getValue();
                String value = scalarNode.getValue();
                return new TaggedValue(tag, value);
            }
            // For non-scalar nodes, use the default constructor
            return constructObject(node);
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

    /**
     * Wrapper for unknown YAML tags to preserve tag information in output
     */
    public static class TaggedValue {
        private final String tag;
        private final String value;

        public TaggedValue(String tag, String value) {
            this.tag = tag;
            this.value = value;
        }

        public String getTag() {
            return tag;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Custom Representer to output TaggedValue with original tags
     */
    public static class TagPreservingRepresenter extends Representer {
        public TagPreservingRepresenter(DumperOptions options) {
            super(options);
            this.representers.put(TaggedValue.class, new RepresentTaggedValue());
        }

        private class RepresentTaggedValue implements Represent {
            @Override
            public Node representData(Object data) {
                TaggedValue tagged = (TaggedValue) data;
                Tag tag = new Tag(tagged.getTag());
                return representScalar(tag, tagged.getValue());
            }
        }
    }
}