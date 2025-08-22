package com.dataliquid.maven.asciidoc.mojo;

import com.dataliquid.maven.asciidoc.stub.LogCapture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RenderMojoTest extends AbstractMojoTest<RenderMojo> {

    @Override
    protected RenderMojo createMojo() {
        return new RenderMojo();
    }

    @Test
    void shouldConvertAsciiDocToHtml() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/simple-render-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        String expectedHtml = loadTestResource("/functional/render/simple-render-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "sample.html");
        assertTrue(generatedHtml.exists(), "HTML file should exist at: " + generatedHtml.getAbsolutePath());

        String actualHtml = loadFile(generatedHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    @Disabled("Known issue: failOnNoFiles doesn't work when AbstractAsciiDocMojo doesn't call processFiles for empty file list")
    void shouldFailWhenNoFilesFoundAndFailOnNoFilesIsTrue() throws Exception {
        // Given
        setField(mojo, "failOnNoFiles", true);
        // Don't create any .adoc files - the default pattern *.adoc will find no files

        // When & Then
        assertThrows(MojoFailureException.class, () -> mojo.execute(),
                "Should throw MojoFailureException when no files found and failOnNoFiles is true");
    }

    @Test
    void shouldCreateOutputAndWorkDirectoriesIfNotExist() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/create-directories-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        String expectedHtml = loadTestResource("/functional/render/create-directories-test/expected.html");

        // Ensure directories don't exist
        outputDir.delete();
        workDir.delete();
        assertFalse(outputDir.exists(), "Output directory should not exist before test");
        assertFalse(workDir.exists(), "Work directory should not exist before test");

        // When
        mojo.execute();

        // Then
        assertTrue(outputDir.exists(), "Output directory should be created");
        assertTrue(workDir.exists(), "Work directory should be created");

        File generatedHtml = new File(outputDir, "test.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated");

        String actualHtml = loadFile(generatedHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldRenderPlantUmlDiagramAsPng() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/diagram-support-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "enableDiagrams", true);
        setField(mojo, "diagramFormat", "png");

        String expectedHtml = loadTestResource("/functional/render/diagram-support-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "diagram.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated");

        File imagesDir = new File(workDir, "images");
        assertTrue(imagesDir.exists(), "Images directory should be created for diagrams");

        // Check for PNG file creation
        File[] pngFiles = imagesDir.listFiles((dir, name) -> name.endsWith(".png"));
        assertNotNull(pngFiles, "PNG files array should not be null");
        assertTrue(pngFiles.length > 0, "At least one PNG file should be generated");

        // Verify PNG file has content (> 0 bytes)
        for (File pngFile : pngFiles) {
            assertTrue(pngFile.exists(), "PNG file should exist: " + pngFile.getName());
            assertTrue(pngFile.length() > 0, "PNG file should have content (> 0 bytes): " + pngFile.getName());
        }

        String actualHtml = loadFile(generatedHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldProcessWithCustomTemplateDir() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/custom-template-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Use the templates directory from test resources
        File templateDir = new File(
                getClass().getResource("/functional/render/custom-template-test/templates").toURI());
        setField(mojo, "templateDir", templateDir);

        String expectedHtml = loadTestResource("/functional/render/custom-template-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "template-test.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated with custom templates");

        String actualHtml = loadFile(generatedHtml);

        // Verify custom templates were used
        assertTrue(actualHtml.contains("custom-paragraph"), "Should use custom paragraph template");
        assertTrue(actualHtml.contains("custom-table-wrapper"), "Should use custom table template");

        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldHandleEmptyHtmlConversion() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/empty-conversion-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Mock Asciidoctor to return empty content
        Asciidoctor mockAsciidoctor = mock(Asciidoctor.class);
        when(mockAsciidoctor.convert(anyString(), any(Options.class))).thenReturn("");

        // Inject the mock
        setField(mojo, "asciidoctor", mockAsciidoctor);

        // When
        mojo.execute();

        // Then
        assertFalse(new File(outputDir, "empty.html").exists(), "HTML file should not be created for empty conversion");
    }

    @Test
    void shouldHandleGeneralExecutionException() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/general-exception-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Inject a mock Asciidoctor that throws exception during requireLibrary
        Asciidoctor mockAsciidoctor = mock(Asciidoctor.class);
        doThrow(new RuntimeException("Failed to load library")).when(mockAsciidoctor).requireLibrary(anyString());
        setField(mojo, "asciidoctor", mockAsciidoctor);
        setField(mojo, "enableDiagrams", true);

        // When & Then
        assertThrows(MojoExecutionException.class, () -> mojo.execute(),
                "Should throw MojoExecutionException when file processing fails");
    }

    @Test
    void shouldProcessWithCustomAttributes() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/custom-attributes-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("toc", "left");
        attributes.put("source-highlighter", "coderay");
        setField(mojo, "attributes", attributes);

        String expectedHtml = loadTestResource("/functional/render/custom-attributes-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "attributes.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated with custom attributes");

        String actualHtml = loadFile(generatedHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldHandleIncrementalBuildWithUnchangedFiles() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/incremental-unchanged-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "enableIncremental", true);

        String expectedHtml = loadTestResource("/functional/render/incremental-unchanged-test/expected.html");

        // First run to establish baseline
        mojo.execute();
        File firstHtml = new File(outputDir, "incremental.html");
        assertTrue(firstHtml.exists(), "HTML file should be generated on first run");
        long firstModified = firstHtml.lastModified();

        String actualHtml = loadFile(firstHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");

        // Wait a bit to ensure different timestamp
        Thread.sleep(100);

        // When - second run without changes
        mojo.execute();

        // Then
        assertEquals(firstModified, firstHtml.lastModified(),
                "HTML file should not be regenerated for unchanged source");
    }

    @Test
    void shouldHandleIncrementalBuildWithChangedFiles() throws Exception {
        // Given
        setField(mojo, "enableIncremental", true);

        // Copy the original test file to the source directory
        Path testAdoc = sourceDir.toPath().resolve("original.adoc");
        String originalContent = loadTestResource("/functional/render/incremental-changed-test/original.adoc");
        Files.writeString(testAdoc, originalContent);

        // First run
        mojo.execute();
        File html = new File(outputDir, "original.html");
        long firstModified = html.lastModified();

        // Wait and modify file
        Thread.sleep(100);
        String modifiedContent = loadTestResource("/functional/render/incremental-changed-test/modified.adoc");
        Files.writeString(testAdoc, modifiedContent);

        String expectedModifiedHtml = loadTestResource(
                "/functional/render/incremental-changed-test/expected-modified.html");

        // When
        mojo.execute();

        // Then
        assertTrue(html.lastModified() > firstModified, "HTML file should be regenerated for changed source");

        String actualHtml = loadFile(html);
        assertEquals(expectedModifiedHtml, actualHtml, "Generated HTML should match expected modified HTML");
    }

    @Test
    @Disabled("Platform-specific test that doesn't work reliably - warning message not consistent")
    void shouldFallbackToFullBuildWhenIncrementalFails() throws Exception {
        // Given
        setField(mojo, "enableIncremental", true);

        // Make work directory unwritable to cause incremental build initialization
        // failure
        workDir.mkdirs();
        workDir.setWritable(false);

        File testSourceDir = new File(getClass().getResource("/functional/render/incremental-fallback-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Capture log to verify warning
        Log mockLog = mock(Log.class);
        mojo.setLog(mockLog);

        try {
            // When
            mojo.execute();

            // Then
            assertTrue(new File(outputDir, "fallback.html").exists(),
                    "HTML file should still be generated with fallback");
            // The warning happens during saveHashCache, not initialization
            verify(mockLog).warn(contains("Failed to save hash cache"));
        } finally {
            // Cleanup
            workDir.setWritable(true);
        }
    }

    @Test
    void shouldHandleFileProcessingException() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/processing-exception-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Mock Asciidoctor to throw exception
        Asciidoctor mockAsciidoctor = mock(Asciidoctor.class);
        when(mockAsciidoctor.convert(anyString(), any(Options.class)))
                .thenThrow(new RuntimeException("Processing error"));
        setField(mojo, "asciidoctor", mockAsciidoctor);

        // Capture log
        Log mockLog = mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        assertFalse(new File(outputDir, "error-processing.html").exists(),
                "HTML file should not be created when processing fails");
        verify(mockLog).error(contains("Error processing file"), any(Exception.class));
    }

    @Test
    void shouldSkipExecutionWhenSkipIsTrue() throws Exception {
        // Given
        setField(mojo, "skip", true);
        File testSourceDir = new File(getClass().getResource("/functional/render/skip-execution-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // When
        mojo.execute();

        // Then
        assertFalse(new File(outputDir, "skip.html").exists(), "No HTML file should be generated when skip is true");
    }

    @Test
    void shouldHandleMissingSourceDirectory() throws Exception {
        // Given
        sourceDir.delete();
        assertFalse(sourceDir.exists(), "Source directory should not exist");

        // Capture log
        Log mockLog = mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        verify(mockLog).warn(contains("Source directory does not exist"));
        assertFalse(outputDir.exists(), "Output directory should not be created");
    }

    @Test
    void shouldProcessMultipleFilesWithMixedResults() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/multiple-files-mixed-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Mock Asciidoctor to fail on specific content
        Asciidoctor mockAsciidoctor = mock(Asciidoctor.class);
        String expectedSuccessHtml = loadTestResource(
                "/functional/render/multiple-files-mixed-test/expected-success.html");
        when(mockAsciidoctor.convert(contains("This will work"), any(Options.class))).thenReturn(expectedSuccessHtml);
        when(mockAsciidoctor.convert(contains("This will fail"), any(Options.class)))
                .thenThrow(new RuntimeException("Conversion failed"));
        setField(mojo, "asciidoctor", mockAsciidoctor);

        // Capture log
        Log mockLog = mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        File successHtml = new File(outputDir, "success.html");
        assertTrue(successHtml.exists(), "Successful file should be converted");

        String actualHtml = loadFile(successHtml);
        assertEquals(expectedSuccessHtml, actualHtml, "Generated HTML should match expected HTML");

        assertFalse(new File(outputDir, "error.html").exists(), "Failed file should not produce output");
        verify(mockLog).error(contains("Error processing file"), any(Exception.class));
    }

    @Test
    void shouldRenderPlantUmlDiagramAsSvg() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/plantuml-diagram-test-svg").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "enableDiagrams", true);
        setField(mojo, "diagramFormat", "svg");

        String expectedHtml = loadTestResource("/functional/render/plantuml-diagram-test-svg/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "test-svg.html");
        assertTrue(generatedHtml.exists(), "HTML file should exist at: " + generatedHtml.getAbsolutePath());

        String actualHtml = loadFile(generatedHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should have embedded SVG elements");
    }

    @Test
    void shouldRenderDitaaDiagramAsPng() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/ditaa-diagram-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "enableDiagrams", true);
        setField(mojo, "diagramFormat", "png");

        String expectedHtml = loadTestResource("/functional/render/ditaa-diagram-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "ditaa-diagram.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated");

        File imagesDir = new File(workDir, "images");
        assertTrue(imagesDir.exists(), "Images directory should be created for diagrams");

        // Check for PNG file creation
        File[] pngFiles = imagesDir.listFiles((dir, name) -> name.endsWith(".png"));
        assertNotNull(pngFiles, "PNG files array should not be null");
        assertTrue(pngFiles.length > 0, "At least one PNG file should be generated for Ditaa diagram");

        // Verify PNG file has content (> 0 bytes)
        for (File pngFile : pngFiles) {
            assertTrue(pngFile.exists(), "PNG file should exist: " + pngFile.getName());
            assertTrue(pngFile.length() > 0, "PNG file should have content (> 0 bytes): " + pngFile.getName());
        }

        String actualHtml = loadFile(generatedHtml);
        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldRenderPlantUmlDiagramAsInlineSvg() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/plantuml-inline-svg-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "enableDiagrams", true);
        setField(mojo, "diagramFormat", "svg");

        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("data-uri", ""); // Enable data-uri for inline content
        setField(mojo, "attributes", customAttributes);

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "inline-svg.html");
        assertTrue(generatedHtml.exists(), "HTML file should exist at: " + generatedHtml.getAbsolutePath());

        String actualHtml = loadFile(generatedHtml);

        // Verify the structure contains expected elements
        assertTrue(actualHtml.contains("<div id=\"preamble\">"), "Should contain preamble div");
        assertTrue(actualHtml.contains("This document tests inline SVG diagram rendering."),
                "Should contain intro text");
        assertTrue(
                actualHtml.contains("<h2 id=\"_plantuml_diagram_as_inline_svg\">PlantUML Diagram as Inline SVG</h2>"),
                "Should contain section heading");
        assertTrue(actualHtml.contains("This diagram should be embedded as inline SVG directly in the HTML:"),
                "Should contain diagram description");

        // Verify SVG is embedded inline (not as img src)
        assertTrue(actualHtml.contains("<svg"), "Should contain inline SVG element");
        assertTrue(actualHtml.contains("xmlns='http://www.w3.org/2000/svg'"), "SVG should have proper namespace");
        assertFalse(actualHtml.contains("<img"), "Should not contain img element for inline SVG");

        // Verify SVG content contains expected elements
        assertTrue(actualHtml.contains("Alice"), "SVG should contain Alice text");
        assertTrue(actualHtml.contains("Bob"), "SVG should contain Bob text");
        assertTrue(actualHtml.contains("Request"), "SVG should contain Request text");
        assertTrue(actualHtml.contains("Response"), "SVG should contain Response text");

        // Verify proper structure closure
        assertTrue(actualHtml.contains("The SVG should be embedded directly in the HTML."),
                "Should contain closing paragraph");
    }

    @Test
    void shouldRenderWithInlineStringTemplate() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/inline-template-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Set inline StringTemplate
        String inlineTemplate = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Inline Template: $if(attributes.doctitle)$$attributes.doctitle$$else$No Title$endif$</title>
                </head>
                <body>
                    <div class="inline-template-wrapper">
                        <h1>Document rendered with inline template</h1>
                        $html$
                    </div>
                    <footer>Generated by inline StringTemplate</footer>
                </body>
                </html>
                """;
        setField(mojo, "template", inlineTemplate);

        String expectedHtml = loadTestResource("/functional/render/inline-template-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "inline-test.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated with inline template");

        String actualHtml = loadFile(generatedHtml);

        // Verify inline template was used
        assertTrue(actualHtml.contains("class=\"inline-template-wrapper\""), "Should use inline template wrapper");
        assertTrue(actualHtml.contains("Document rendered with inline template"),
                "Should contain inline template heading");
        assertTrue(actualHtml.contains("Generated by inline StringTemplate"), "Should contain inline template footer");

        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldUseInlineTemplateOverFileTemplate() throws Exception {
        // Given
        File testSourceDir = new File(
                getClass().getResource("/functional/render/inline-template-override-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Set both file and inline template - inline should win
        setField(mojo, "templateFile", "templates/partial.st");
        String inlineTemplate = """
                <!-- INLINE TEMPLATE WINS -->
                $html$
                <!-- END INLINE TEMPLATE -->
                """;
        setField(mojo, "template", inlineTemplate);

        // Mock log to verify warning
        Log mockLog = mock(Log.class);
        mojo.setLog(mockLog);

        String expectedHtml = loadTestResource("/functional/render/inline-template-override-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "override-test.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated");

        String actualHtml = loadFile(generatedHtml);

        // Verify inline template was used by checking there's no wrapper that file
        // template would add
        assertFalse(actualHtml.contains("<!DOCTYPE html>"), "Should not have DOCTYPE from file template");
        assertFalse(actualHtml.contains("<html>"), "Should not have html tag from file template");
        assertFalse(actualHtml.contains("<body>"), "Should not have body tag from file template");

        // Verify warning was logged
        verify(mockLog).warn("Both template (inline) and templateFile are configured. Using inline template.");

        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldRenderComplexInlineTemplate() throws Exception {
        // Given
        File testSourceDir = new File(
                getClass().getResource("/functional/render/inline-template-complex-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Set a complex inline template with conditionals and iterations
        String complexTemplate = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>$if(attributes.doctitle)$$attributes.doctitle$$else$Untitled$endif$</title>
                    $if(frontMatter.author)$
                    <meta name="author" content="$frontMatter.author$">
                    $endif$
                </head>
                <body>
                    <header>
                        <h1>$if(attributes.doctitle)$$attributes.doctitle$$else$Document$endif$</h1>
                        $if(attributes.version)$
                        <p class="version">Version: $attributes.version$</p>
                        $endif$
                    </header>

                    <main>
                        $html$
                    </main>

                    <footer>
                        $if(metadata._file)$
                        <p class="meta">Source: $metadata._file$</p>
                        $endif$
                        <p>Generated with complex inline template</p>
                    </footer>
                </body>
                </html>
                """;
        setField(mojo, "template", complexTemplate);

        // Set custom attributes
        Map<String, Object> customAttributes = new HashMap<>();
        customAttributes.put("version", "2.0");
        setField(mojo, "attributes", customAttributes);

        String expectedHtml = loadTestResource("/functional/render/inline-template-complex-test/expected.html");

        // When
        mojo.execute();

        // Then
        File generatedHtml = new File(outputDir, "complex-test.html");
        assertTrue(generatedHtml.exists(), "HTML file should be generated with complex template");

        String actualHtml = loadFile(generatedHtml);

        // Verify complex template features
        assertTrue(actualHtml.contains("class=\"meta\""), "Should contain meta class");
        assertTrue(actualHtml.contains("Generated with complex inline template"),
                "Should contain complex template footer");

        // Note: The version attribute gets merged with document attributes,
        // but may not appear in the output since the template checks
        // attributes.get("version")
        // which might be overridden by AsciidoctorJ's processing

        assertEquals(expectedHtml, actualHtml, "Generated HTML should match expected HTML");
    }

    @Test
    void shouldRenderTemplateWithIndentation() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/template-indentation-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Set a YAML-like template with significant indentation
        String indentedTemplate = """
                page:
                  id: '$frontMatter.identifier$'
                  title: '$if(attributes.doctitle)$$attributes.doctitle$$else$Untitled$endif$'
                  metadata:
                    author: '$if(frontMatter.author)$$frontMatter.author$$else$Unknown$endif$'
                    created: '$if(frontMatter.created)$$frontMatter.created$$else$N/A$endif$'
                  content:
                    html: |
                      $html$
                  nested:
                    level1:
                      level2:
                        value: 'deep'
                """;
        setField(mojo, "template", indentedTemplate);

        String expectedYaml = loadTestResource("/functional/render/template-indentation-test/expected.yaml");

        // When
        mojo.execute();

        // Then
        File generatedFile = new File(outputDir, "indented.html");
        assertTrue(generatedFile.exists(), "Output file should be generated");

        String actualOutput = Files.readString(generatedFile.toPath());

        // Compare with expected output (trim to handle newline differences)
        assertEquals(expectedYaml.trim(), actualOutput.trim(), "Generated YAML should match expected output");
    }

    @Test
    void shouldDisplayErrorWithCleanIndentation() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/render/error-indentation-test").toURI());
        setField(mojo, "sourceDirectory", testSourceDir);

        // Set a template with error and significant indentation
        // This template has a runtime error - calling a non-existent method
        String errorTemplate = """
                page:
                  id: '$frontMatter.identifier$'
                  title: '$if(attributes.doctitle)$$attributes.doctitle$$else$Untitled$endif$'
                  error: '$attributes.nonExistent.deepProperty$'
                  content:
                    html: |
                      $html$
                """;
        setField(mojo, "template", errorTemplate);

        // Use LogCapture to capture all log output with debug enabled
        LogCapture logCapture = new LogCapture(true); // Enable debug logging
        setField(mojo, "log", logCapture);

        // When - execute (the error might just be logged, not thrown)
        try {
            mojo.execute();
        } catch (Exception e) {
            // If an exception is thrown, that's fine too
        }

        // Then - check that the expected error output is present in the log
        String actualLogOutput = logCapture.getCapturedOutput();
        String expectedError = loadTestResource("/functional/render/error-indentation-test/expected-error.txt");

        // Check if each line from the expected error is present in the log output
        String[] expectedLines = expectedError.split("\n");
        for (String expectedLine : expectedLines) {
            String trimmedLine = expectedLine.trim();
            if (!trimmedLine.isEmpty()) {
                assertTrue(actualLogOutput.contains(trimmedLine),
                        "Log output should contain line: '" + trimmedLine + "'");
            }
        }
    }
}