package com.dataliquid.maven.asciidoc.mojo;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        
        verify(mockLog).info(eq("Linting complete:"));
        
        verify(mockLog).info(eq("  Errors: 0"));
        verify(mockLog).info(eq("  Warnings: 0"));
        
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
        verify(mockLog).info(eq("Linting complete:"));
        
        verify(mockLog).info(eq("  Errors: 1"));
        verify(mockLog).info(eq("  Warnings: 0"));
        
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
        MojoExecutionException exception = assertThrows(MojoExecutionException.class, 
            () -> mojo.execute(),
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
        setField(mojo, "outputFormat", "enhanced");
        setField(mojo, "useColors", false);
        setField(mojo, "contextLines", 2);
        setField(mojo, "showSuggestions", true);
        setField(mojo, "highlightErrors", true);
        setField(mojo, "showExamples", false);
        
        com.dataliquid.maven.asciidoc.stub.LogCapture logCapture = new com.dataliquid.maven.asciidoc.stub.LogCapture();
        mojo.setLog(logCapture);
        
        // When
        mojo.execute();
        
        // Then
        String expectedOutput = 
            "[INFO] Starting AsciiDoc linting...\n" +
            "[INFO] Found 1 AsciiDoc files\n" +
            "[INFO] Found 1 AsciiDoc files to lint\n" +
            "[INFO] Loading linter configuration from: " + ruleFile.getAbsolutePath() + "\n" +
            "[INFO] Linting: " + documentPath + "\n" +
            "[INFO] " + documentPath + ":\n" +
            "[INFO] \n" +
            "[ERROR] [ERROR]: Attribute 'keywords' is too long: actual 'This is a very long keywords list that exceeds the maximum allowed' (66 characters), expected maximum 20 characters [metadata.length.max]\n" +
            "[ERROR]   File: " + documentPath + ":2:12-77\n" +
            "[ERROR]   Actual: This is a very long keywords list that exceeds the maximum allowed (66 characters)\n" +
            "[ERROR]   Expected: Maximum 20 characters\n" +
            "[ERROR] \n" +
            "[ERROR]    1 | = Document\n" +
            "[ERROR]    2 | :keywords: This is a very long keywords list that exceeds the maximum allowed\n" +
            "[ERROR]      |            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~\n" +
            "[ERROR]    3 | \n" +
            "[ERROR]    4 | Content here.\n" +
            "[ERROR] \n" +
            "[ERROR] Suggested fix:\n" +
            "[ERROR]   Shorten the attribute value\n" +
            "[ERROR]   Attribute value must not exceed 20 characters\n" +
            "[ERROR] \n" +
            "[ERROR] \n" +
            "[ERROR] \n" +
            "[ERROR] +----------------------------------------------------------------------------------------------------------------------+\n" +
            "[ERROR] |                                                  Validation Summary                                                  |\n" +
            "[ERROR] +----------------------------------------------------------------------------------------------------------------------+\n" +
            "[ERROR]   Total files scanned:     1\n" +
            "[ERROR]   Files with errors:       1\n" +
            "[ERROR] \n" +
            "[ERROR]   Errors:   1\n" +
            "[ERROR]   Warnings: 0\n" +
            "[ERROR]   Info:     0\n" +
            "[ERROR] \n" +
            "[ERROR]   Most common issues:\n" +
            "[ERROR]   💡   - Attribute 'keywords' is too long: actual 'This is a very long keywords list that exceeds the maximum allowed' (66 characters), expected maximum 20 characters (1 occurrence)\n" +
            "[ERROR] \n" +
            "[ERROR] \n" +
            "[ERROR] Summary: 1 error, 0 warnings, 0 info messages\n" +
            "[ERROR] Validation completed in TIMING_PLACEHOLDERms\n" +
            "[ERROR] +----------------------------------------------------------------------------------------------------------------------+\n" +
            "[INFO] Linting complete:\n" +
            "[INFO]   Errors: 1\n" +
            "[INFO]   Warnings: 0\n";
        
        String actualOutput = logCapture.getCapturedOutput();
        
        String actualNormalized = actualOutput.replaceAll("Validation completed in \\d+ms", "Validation completed in TIMING_PLACEHOLDERms");
        
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
        setField(mojo, "outputFormat", "enhanced");
        setField(mojo, "useColors", false);
        setField(mojo, "contextLines", 2);
        setField(mojo, "showSuggestions", true);
        setField(mojo, "highlightErrors", true);
        setField(mojo, "showExamples", false);
        
        com.dataliquid.maven.asciidoc.stub.LogCapture logCapture = new com.dataliquid.maven.asciidoc.stub.LogCapture();
        mojo.setLog(logCapture);
        
        // When
        mojo.execute();
        
        // Then
        String expectedOutput = 
            "[INFO] Starting AsciiDoc linting...\n" +
            "[INFO] Found 1 AsciiDoc files\n" +
            "[INFO] Found 1 AsciiDoc files to lint\n" +
            "[INFO] Loading linter configuration from: " + ruleFile.getAbsolutePath() + "\n" +
            "[INFO] Linting: " + documentPath + "\n" +
            "[INFO] " + documentPath + ":\n" +
            "[INFO] \n" +
            "[ERROR] [ERROR]: Missing required attribute 'description' [metadata.required]\n" +
            "[ERROR]   File: " + documentPath + ":4:1\n" +
            "[ERROR]   Expected: Attribute must be present\n" +
            "[ERROR] \n" +
            "[ERROR]    2 | :version: 1.0.0\n" +
            "[ERROR]    3 | :keywords: API, REST, Documentation\n" +
            "[ERROR]    4 | «:description: value»\n" +
            "[ERROR]    5 | \n" +
            "[ERROR]    6 | == Overview\n" +
            "[ERROR]    7 | \n" +
            "[ERROR] \n" +
            "[ERROR] Suggested fix:\n" +
            "[ERROR]   Add required attribute to document header\n" +
            "[ERROR]   :description: value\n" +
            "[ERROR]   Required attributes must be defined in the document header\n" +
            "[ERROR] \n" +
            "[ERROR] [ERROR]: Missing required attribute 'author' [metadata.required]\n" +
            "[ERROR]   File: " + documentPath + ":4:1\n" +
            "[ERROR]   Expected: Attribute must be present\n" +
            "[ERROR] \n" +
            "[ERROR]    2 | :version: 1.0.0\n" +
            "[ERROR]    3 | :keywords: API, REST, Documentation\n" +
            "[ERROR]    4 | «:author: value»\n" +
            "[ERROR]    5 | \n" +
            "[ERROR]    6 | == Overview\n" +
            "[ERROR]    7 | \n" +
            "[ERROR] \n" +
            "[ERROR] Suggested fix:\n" +
            "[ERROR]   Add required attribute to document header\n" +
            "[ERROR]   :author: value\n" +
            "[ERROR]   Required attributes must be defined in the document header\n" +
            "[ERROR] \n" +
            "[ERROR] \n" +
            "[ERROR] \n" +
            "[ERROR] +----------------------------------------------------------------------------------------------------------------------+\n" +
            "[ERROR] |                                                  Validation Summary                                                  |\n" +
            "[ERROR] +----------------------------------------------------------------------------------------------------------------------+\n" +
            "[ERROR]   Total files scanned:     1\n" +
            "[ERROR]   Files with errors:       1\n" +
            "[ERROR] \n" +
            "[ERROR]   Errors:   2\n" +
            "[ERROR]   Warnings: 0\n" +
            "[ERROR]   Info:     0\n" +
            "[ERROR] \n" +
            "[ERROR]   Most common issues:\n" +
            "[ERROR]   💡   💡   - Missing required attribute 'description' (2 occurrences)\n" +
            "[ERROR] \n" +
            "[ERROR] \n" +
            "[ERROR] Summary: 2 errors, 0 warnings, 0 info messages\n" +
            "[ERROR] Validation completed in TIMING_PLACEHOLDERms\n" +
            "[ERROR] +----------------------------------------------------------------------------------------------------------------------+\n" +
            "[INFO] Linting complete:\n" +
            "[INFO]   Errors: 2\n" +
            "[INFO]   Warnings: 0\n";
        
        String actualOutput = logCapture.getCapturedOutput();
        
        String actualNormalized = actualOutput.replaceAll("Validation completed in \\d+ms", "Validation completed in TIMING_PLACEHOLDERms");
        
        assertEquals(expectedOutput, actualNormalized, 
            "Placeholder enforcement output must match EXACTLY - every character, every space, every line!");
    }
}