package com.dataliquid.maven.asciidoc.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlAsciiDocProcessorTest {

    private YamlAsciiDocProcessor processor;
    private Log mockLog;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mockLog = mock(Log.class);
        // For extraction-only operations, use the simplified constructor
        processor = new YamlAsciiDocProcessor(mockLog);
    }

    @Test
    void shouldExtractSingleAsciiDocContent() throws IOException {
        // given
        String yaml = """
                title: Test Document
                content: !asciidoc |
                  = My Document

                  This is a test document.
                """;
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, yaml);

        // when
        List<YamlAsciiDocProcessor.ExtractedContent> extracted = processor.extractAsciiDocContent(yamlFile);

        // then
        assertEquals(1, extracted.size());
        assertEquals("content", extracted.get(0).getYamlPath());
        assertTrue(extracted.get(0).getContent().contains("= My Document"));
    }

    @Test
    void shouldExtractMultipleAsciiDocContents() throws IOException {
        // given
        String yaml = """
                sections:
                  - name: Introduction
                    text: !asciidoc |
                      == Introduction

                      This is the intro.
                  - name: Conclusion
                    text: !asciidoc |
                      == Conclusion

                      This is the conclusion.
                """;
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, yaml);

        // when
        List<YamlAsciiDocProcessor.ExtractedContent> extracted = processor.extractAsciiDocContent(yamlFile);

        // then
        assertEquals(2, extracted.size());
        assertEquals("sections[0].text", extracted.get(0).getYamlPath());
        assertEquals("sections[1].text", extracted.get(1).getYamlPath());
        assertTrue(extracted.get(0).getContent().contains("== Introduction"));
        assertTrue(extracted.get(1).getContent().contains("== Conclusion"));
    }

    @Test
    void shouldExtractNestedAsciiDocContent() throws IOException {
        // given
        String yaml = """
                api:
                  documentation:
                    overview: !asciidoc |
                      = API Overview

                      REST API documentation.
                    endpoints:
                      users:
                        description: !asciidoc |
                          == User Endpoints

                          User management endpoints.
                """;
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, yaml);

        // when
        List<YamlAsciiDocProcessor.ExtractedContent> extracted = processor.extractAsciiDocContent(yamlFile);

        // then
        assertEquals(2, extracted.size());
        assertEquals("api.documentation.overview", extracted.get(0).getYamlPath());
        assertEquals("api.documentation.endpoints.users.description", extracted.get(1).getYamlPath());
    }

    @Test
    void shouldHandleYamlWithoutAsciiDocTags() throws IOException {
        // given
        String yaml = """
                title: Regular YAML
                description: No AsciiDoc content here
                items:
                  - name: Item 1
                  - name: Item 2
                """;
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, yaml);

        // when
        List<YamlAsciiDocProcessor.ExtractedContent> extracted = processor.extractAsciiDocContent(yamlFile);

        // then
        assertTrue(extracted.isEmpty());
    }

    @Test
    void shouldHandleComplexYamlPaths() throws IOException {
        // given
        String yaml = """
                documentation:
                  chapters:
                    - title: Chapter 1
                      sections:
                        - content: !asciidoc |
                            == Section 1.1
                        - content: !asciidoc |
                            == Section 1.2
                    - title: Chapter 2
                      sections:
                        - content: !asciidoc |
                            == Section 2.1
                """;
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, yaml);

        // when
        List<YamlAsciiDocProcessor.ExtractedContent> extracted = processor.extractAsciiDocContent(yamlFile);

        // then
        assertEquals(3, extracted.size());
        assertEquals("documentation.chapters[0].sections[0].content", extracted.get(0).getYamlPath());
        assertEquals("documentation.chapters[0].sections[1].content", extracted.get(1).getYamlPath());
        assertEquals("documentation.chapters[1].sections[0].content", extracted.get(2).getYamlPath());
    }

    @Test
    void shouldHandleEmptyAsciiDocContent() throws IOException {
        // given
        String yaml = """
                content: !asciidoc |
                """;
        Path yamlFile = tempDir.resolve("test.yaml");
        Files.writeString(yamlFile, yaml);

        // when
        List<YamlAsciiDocProcessor.ExtractedContent> extracted = processor.extractAsciiDocContent(yamlFile);

        // then
        assertEquals(1, extracted.size());
        assertEquals("content", extracted.get(0).getYamlPath());
        assertNotNull(extracted.get(0).getContent());
        assertTrue(extracted.get(0).getContent().isEmpty() || extracted.get(0).getContent().isBlank());
    }
}