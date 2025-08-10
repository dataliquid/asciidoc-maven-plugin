package com.dataliquid.maven.asciidoc.mojo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
        // LinterMojo specific defaults
        setField(mojo, "failOnError", true);
    }
    
    @Test
    void shouldLintAsciiDocFilesSuccessfully() throws Exception {
        // Given
        File testSourceDir = new File(getClass().getResource("/functional/lint/simple-lint-test").toURI());
        File ruleFile = new File(testSourceDir, "linter-rules.yaml");
        
        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "ruleFile", ruleFile);
        
        // Use a mock log to capture output
        Log mockLog = Mockito.mock(Log.class);
        mojo.setLog(mockLog);
        
        // When
        mojo.execute();
        
        // Then
        // Verify the linter was executed
        verify(mockLog).info(eq("Found 1 AsciiDoc files to lint"));
        verify(mockLog).info(eq("Loading linter configuration from: " + ruleFile.getAbsolutePath()));
        verify(mockLog).info(contains("Linting: "));
        verify(mockLog).info(contains("document.adoc"));
        
        // Verify linting completed successfully
        verify(mockLog).info(eq("Linting complete:"));
        
        // Verify no errors or warnings occurred
        verify(mockLog).info(eq("  Errors: 0"));
        verify(mockLog).info(eq("  Warnings: 0"));
        
        // Verify no error or warning messages were logged for the document
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
        setField(mojo, "failOnError", false); // Don't fail on errors for this test
        
        // Use a mock log to capture output
        Log mockLog = Mockito.mock(Log.class);
        mojo.setLog(mockLog);
        
        // When
        mojo.execute();
        
        // Then
        // Verify the linter was executed
        verify(mockLog).info(eq("Starting AsciiDoc linting..."));
        verify(mockLog).info(eq("Found 1 AsciiDoc files"));
        verify(mockLog).info(eq("Found 1 AsciiDoc files to lint"));
        verify(mockLog).info(eq("Loading linter configuration from: " + ruleFile.getAbsolutePath()));
        
        // Verify linting completed with violations
        verify(mockLog).info(eq(""));
        verify(mockLog).info(eq("Linting complete:"));
        
        // Verify exact error and warning counts
        verify(mockLog).info(eq("  Errors: 1"));
        verify(mockLog).info(eq("  Warnings: 0"));
        
        // Verify error message was logged
        verify(mockLog).error(eq("[document.adoc:2] ERROR: Attribute 'keywords' is too long: actual 'This is a very long keywords list that exceeds the maximum allowed' (66 characters), expected maximum 20 characters"));
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
}