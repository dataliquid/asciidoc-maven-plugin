package com.dataliquid.maven.asciidoc.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LinterMojoTest extends AbstractMojoTest<LinterMojo> {

    @Override
    protected LinterMojo createMojo() {
        return new LinterMojo();
    }

    @Override
    protected void configureDefaultMojo(LinterMojo mojo) throws Exception {
        super.configureDefaultMojo(mojo);
        setField(mojo, "failOnError", true);
        setField(mojo, "consoleOutputFormat", "enhanced");
    }

    @Test
    void shouldLintAsciiDocFilesSuccessfully() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/simple-lint-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);

        Log mockLog = Mockito.mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        verify(mockLog).info(eq("Found 1 AsciiDoc files to lint"));
        verify(mockLog).info(eq("Loading linter configuration from: " + ruleFile.getAbsolutePath()));
        verify(mockLog).info(contains("Linting: "));
        verify(mockLog).info(contains("document.adoc"));

        verify(mockLog, never()).error(contains("document.adoc"));
        verify(mockLog, never()).warn(contains("document.adoc"));
    }

    @Test
    void shouldLintAsciiDocFilesWithViolations() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/lint-with-violations-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "failOnError", false);

        Log mockLog = Mockito.mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        verify(mockLog).info(eq("Starting AsciiDoc linting..."));
        verify(mockLog).info(eq("Found 1 AsciiDoc files"));
        verify(mockLog).info(eq("Found 1 AsciiDoc files to lint"));
        verify(mockLog).info(eq("Loading linter configuration from: " + ruleFile.getAbsolutePath()));

        verify(mockLog, atLeastOnce()).info(eq(""));
        verify(mockLog, atLeastOnce()).error(contains("Attribute 'keywords' is too long"));
    }

    @Test
    void shouldFailWhenRuleFileDoesNotExist() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/simple-lint-test").toURI());
        File nonExistentRuleFile = new File(testSourceDir, "non-existent-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", nonExistentRuleFile);

        // When & Then
        MojoExecutionException exception = assertThrows(MojoExecutionException.class, () -> mojo.execute(),
                "Should throw MojoExecutionException when rule file doesn't exist");

        assertTrue(exception.getMessage().contains("Rule file not found"),
                "Exception message should indicate rule file not found");
    }

    @Test
    void shouldProduceExactEnhancedConsoleOutputWithFullFormattingFeatures() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/lint-with-violations-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");
        String documentPath = testSourceDir.getAbsolutePath() + "/document.adoc";

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "failOnError", false);
        setField(mojo, "consoleOutputFormat", "enhanced");
        setField(mojo, "useColors", false);
        setField(mojo, "contextLines", 2);
        setField(mojo, "showSuggestions", true);
        setField(mojo, "highlightErrors", true);
        setField(mojo, "showExamples", false);
        setField(mojo, "maxSuggestionsPerError", 3);

        com.dataliquid.maven.asciidoc.stub.LogCapture logCapture = new com.dataliquid.maven.asciidoc.stub.LogCapture();
        mojo.setLog(logCapture);

        // When
        mojo.execute();

        // Then
        String expectedOutput = ("""
                [INFO] Starting AsciiDoc linting...
                [INFO] Found 1 AsciiDoc files
                [INFO] Found 1 AsciiDoc files to lint
                [INFO] Loading linter configuration from: %s
                [INFO] Linting: %s
                [INFO] %s:
                [INFO]\s
                [ERROR] [ERROR]: Attribute 'keywords' is too long: actual 'This is a very long keywords list that exceeds the maximum allowed' (66 characters), expected maximum 20 characters [metadata.length.max]
                [ERROR]   File: %s:2:12-77
                [ERROR]   Actual: This is a very long keywords list that exceeds the maximum allowed (66 characters)
                [ERROR]   Expected: Maximum 20 characters
                [ERROR]\s
                [ERROR]    1 | = Document
                [ERROR]    2 | :keywords: This is a very long keywords list that exceeds the maximum allowed
                [ERROR]      |            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                [ERROR]    3 |\s
                [ERROR]    4 | Content here.
                [ERROR]\s
                [ERROR] Suggested fix:
                [ERROR]   Shorten the attribute value
                [ERROR]   Attribute value must not exceed 20 characters
                [ERROR]\s
                [ERROR]\s
                [ERROR]\s
                [ERROR] +----------------------------------------------------------------------------------------------------------------------+
                [ERROR] |                                                  Validation Summary                                                  |
                [ERROR] +----------------------------------------------------------------------------------------------------------------------+
                [ERROR]   Total files scanned:     1
                [ERROR]   Files with errors:       1
                [ERROR]\s
                [ERROR]   Errors:   1
                [ERROR]   Warnings: 0
                [ERROR]   Info:     0
                [ERROR]\s
                [ERROR]   Most common issues:
                [ERROR]   💡   - Attribute 'keywords' is too long: actual 'This is a very long keywords list that exceeds the maximum allowed' (66 characters), expected maximum 20 characters (1 occurrence)
                [ERROR]\s
                [ERROR]\s
                [ERROR] Summary: 1 error, 0 warnings, 0 info messages
                [ERROR] Validation completed in TIMING_PLACEHOLDERms
                [ERROR] +----------------------------------------------------------------------------------------------------------------------+
                """
                .formatted(ruleFile.getAbsolutePath(), documentPath, documentPath, documentPath));

        String actualOutput = logCapture.getCapturedOutput();

        String actualNormalized = actualOutput
                .replaceAll("Validation completed in \\d+ms", "Validation completed in TIMING_PLACEHOLDERms");

        assertEquals(expectedOutput, actualNormalized,
                "Enhanced console output must match exactly including underlines, context, and suggestions");
    }

    @Test
    void shouldEnforcePlaceholderViolationsWithExactOutput() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/placeholder-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");
        String documentPath = testSourceDir.getAbsolutePath() + "/document.adoc";

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "failOnError", false);
        setField(mojo, "consoleOutputFormat", "enhanced");
        setField(mojo, "useColors", false);
        setField(mojo, "contextLines", 2);
        setField(mojo, "showSuggestions", true);
        setField(mojo, "highlightErrors", true);
        setField(mojo, "showExamples", false);
        setField(mojo, "maxSuggestionsPerError", 3);

        com.dataliquid.maven.asciidoc.stub.LogCapture logCapture = new com.dataliquid.maven.asciidoc.stub.LogCapture();
        mojo.setLog(logCapture);

        // When
        mojo.execute();

        // Then
        String expectedOutput = ("""
                [INFO] Starting AsciiDoc linting...
                [INFO] Found 1 AsciiDoc files
                [INFO] Found 1 AsciiDoc files to lint
                [INFO] Loading linter configuration from: %s
                [INFO] Linting: %s
                [INFO] %s:
                [INFO]\s
                [ERROR] [ERROR]: Missing required attribute 'description' [metadata.required]
                [ERROR]   File: %s:4:1
                [ERROR]   Expected: Attribute must be present
                [ERROR]\s
                [ERROR]    2 | :version: 1.0.0
                [ERROR]    3 | :keywords: API, REST, Documentation
                [ERROR]    4 | «:description: value»
                [ERROR]    5 |\s
                [ERROR]    6 | == Overview
                [ERROR]    7 |\s
                [ERROR]\s
                [ERROR] Suggested fix:
                [ERROR]   Add required attribute to document header
                [ERROR]   :description: value
                [ERROR]   Required attributes must be defined in the document header
                [ERROR]\s
                [ERROR] [ERROR]: Missing required attribute 'author' [metadata.required]
                [ERROR]   File: %s:4:1
                [ERROR]   Expected: Attribute must be present
                [ERROR]\s
                [ERROR]    2 | :version: 1.0.0
                [ERROR]    3 | :keywords: API, REST, Documentation
                [ERROR]    4 | «:author: value»
                [ERROR]    5 |\s
                [ERROR]    6 | == Overview
                [ERROR]    7 |\s
                [ERROR]\s
                [ERROR] Suggested fix:
                [ERROR]   Add required attribute to document header
                [ERROR]   :author: value
                [ERROR]   Required attributes must be defined in the document header
                [ERROR]\s
                [ERROR]\s
                [ERROR]\s
                [ERROR] +----------------------------------------------------------------------------------------------------------------------+
                [ERROR] |                                                  Validation Summary                                                  |
                [ERROR] +----------------------------------------------------------------------------------------------------------------------+
                [ERROR]   Total files scanned:     1
                [ERROR]   Files with errors:       1
                [ERROR]\s
                [ERROR]   Errors:   2
                [ERROR]   Warnings: 0
                [ERROR]   Info:     0
                [ERROR]\s
                [ERROR]   Most common issues:
                [ERROR]   💡   💡   - Missing required attribute 'description' (2 occurrences)
                [ERROR]\s
                [ERROR]\s
                [ERROR] Summary: 2 errors, 0 warnings, 0 info messages
                [ERROR] Validation completed in TIMING_PLACEHOLDERms
                [ERROR] +----------------------------------------------------------------------------------------------------------------------+
                """
                .formatted(ruleFile.getAbsolutePath(), documentPath, documentPath, documentPath, documentPath));

        String actualOutput = logCapture.getCapturedOutput();

        String actualNormalized = actualOutput
                .replaceAll("Validation completed in \\d+ms", "Validation completed in TIMING_PLACEHOLDERms");

        assertEquals(expectedOutput, actualNormalized,
                "Placeholder enforcement output must match EXACTLY - every character, every space, every line!");
    }

    @Test
    void shouldLintYamlFilesWithAsciiDocContent() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/yaml-lint-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "includes", new String[] { "**/document.yaml" });
        setField(mojo, "consoleOutputFormat", "enhanced");
        setField(mojo, "useColors", false);
        setField(mojo, "failOnError", true);

        com.dataliquid.maven.asciidoc.stub.LogCapture logCapture = new com.dataliquid.maven.asciidoc.stub.LogCapture();
        mojo.setLog(logCapture);

        // When
        mojo.execute();

        // Then
        String output = logCapture.getCapturedOutput();

        // Verify basic processing
        assertTrue(output.contains("Found 1 AsciiDoc files to lint"), "Should find 1 YAML file");
        assertTrue(output.contains("Loading linter configuration from:"), "Should load config");
        assertTrue(output.contains("Linting YAML file:"), "Should process YAML file");
        assertTrue(output.contains("document.yaml"), "Should mention file name");
        assertTrue(output.contains("Found 3 AsciiDoc content blocks"), "Should find 3 blocks");

        // Verify that doctype attribute is found in at least one block (it's optional
        // per our rules)
        // Since it's info level and not required, we shouldn't see error messages
        assertFalse(output.contains("[ERROR]"), "Should have no errors");
        assertFalse(output.contains("Missing required attribute"), "Should have no missing attributes");

        // The successful linting doesn't show specific paths in info level
        // Only errors would show the detailed YAML paths, so we just verify no errors
        // Debug output would show the paths but isn't captured by LogCapture at INFO
        // level
    }

    @Test
    void shouldReportYamlPathInErrorMessages() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/yaml-with-violations").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "includes", new String[] { "**/content.yaml" });
        setField(mojo, "failOnError", false);
        setField(mojo, "consoleOutputFormat", "enhanced");
        setField(mojo, "useColors", false);

        com.dataliquid.maven.asciidoc.stub.LogCapture logCapture = new com.dataliquid.maven.asciidoc.stub.LogCapture();
        mojo.setLog(logCapture);

        // When
        mojo.execute();

        // Then
        String output = logCapture.getCapturedOutput();

        // Verify YAML file processing
        assertTrue(output.contains("Linting YAML file:"), "Should log YAML file linting");
        assertTrue(output.contains("content.yaml"), "Should mention the YAML file name");
        assertTrue(output.contains("Found 3 AsciiDoc content blocks"), "Should find 3 blocks");

        // Verify YAML paths are included for each block with errors
        assertTrue(output.contains("YAML path: documentation.intro"), "Should show YAML path for first block");
        assertTrue(output.contains("YAML path: api.endpoints[0].description"),
                "Should show YAML path for second block");

        // Verify actual linting errors are reported
        assertTrue(
                output.contains("Missing required attribute 'author'")
                        || output.contains("required property 'author' not found"),
                "Should report missing author attribute");
        assertTrue(
                output.contains("Missing required attribute 'keywords'")
                        || output.contains("required property 'keywords' not found"),
                "Should report missing keywords attribute");

        // The third block (api.endpoints[1].description) should not produce errors
        // since it has all required attributes (author and keywords)
        int errorCount = output.split("Missing required attribute").length - 1;
        assertTrue(errorCount >= 2, "Should have at least 2 errors for missing attributes");
    }

    @Test
    void shouldHandleMixedAdocAndYamlFiles() throws Exception {
        // Given - create a test directory with both .adoc and .yaml files
        File testSourceDir = new File(getClass().getResource("/functional/lint/simple-lint-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "includes", new String[] { "**/*.adoc", "**/*.yaml", "**/*.yml" });

        Log mockLog = Mockito.mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        verify(mockLog).info(contains("Linting:"));
        verify(mockLog).info(contains(".adoc"));
        // If there are YAML files, they would be processed too
    }

    @Test
    void shouldSkipYamlFilesWithoutAsciiDocTags() throws Exception {
        // Given - using the linter-rules.yaml which doesn't have !asciidoc tags
        File testSourceDir = new File(getClass().getResource("/functional/lint/yaml-lint-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        setField(mojo, "includes", new String[] { "linter-rules.yaml" });

        Log mockLog = Mockito.mock(Log.class);
        mojo.setLog(mockLog);

        // When
        mojo.execute();

        // Then
        verify(mockLog).info(contains("Linting YAML file:"));
        verify(mockLog).debug(contains("No !asciidoc tags found in:"));
    }
}
