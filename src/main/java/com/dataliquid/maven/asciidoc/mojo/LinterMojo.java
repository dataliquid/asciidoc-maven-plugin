package com.dataliquid.maven.asciidoc.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.dataliquid.asciidoc.linter.Linter;
import com.dataliquid.asciidoc.linter.config.LinterConfiguration;
import com.dataliquid.asciidoc.linter.config.common.Severity;
import com.dataliquid.asciidoc.linter.config.loader.ConfigurationLoader;
import com.dataliquid.asciidoc.linter.validator.ValidationMessage;
import com.dataliquid.asciidoc.linter.validator.ValidationResult;

/**
 * Goal to lint AsciiDoc files using asciidoc-linter.
 */
@Mojo(name = "lint")
public class LinterMojo extends AbstractAsciiDocMojo {

    @Parameter(property = "asciidoc.linter.ruleFile", required = true)
    private File ruleFile;

    @Parameter(property = "asciidoc.linter.failOnError", defaultValue = "true")
    private boolean failOnError;

    @Override
    protected String getMojoName() {
        return "AsciiDoc linting";
    }

    @Override
    protected void processFiles(List<Path> adocFiles) throws MojoExecutionException, MojoFailureException {
        getLog().info("Found " + adocFiles.size() + " AsciiDoc files to lint");

        try {
            // Create linter instance
            Linter linter = new Linter();
            
            // Load configuration from rule file
            if (!ruleFile.exists()) {
                throw new MojoExecutionException("Rule file not found: " + ruleFile.getAbsolutePath());
            }
            
            getLog().info("Loading linter configuration from: " + ruleFile.getAbsolutePath());
            ConfigurationLoader configLoader = new ConfigurationLoader();
            LinterConfiguration linterConfiguration = configLoader.loadConfiguration(ruleFile.toPath());
            
            int totalErrors = 0;
            int totalWarnings = 0;
            
            // Lint each file
            for (Path file : adocFiles) {
                getLog().info("Linting: " + file);
                
                try {
                    ValidationResult result = linter.validateFile(file, linterConfiguration);
                    
                    // Process results
                    if (!result.getMessages().isEmpty()) {
                        getLog().info("Results for: " + file);
                        
                        for (ValidationMessage validationMessage : result.getMessages()) {
                            int lineNumber = validationMessage.getLocation() != null ? validationMessage.getLocation().getStartLine() : 0;
                            String logMessage = String.format("[%s:%d] %s: %s", 
                                file.getFileName(), 
                                lineNumber, 
                                validationMessage.getSeverity(), 
                                validationMessage.getMessage());
                                
                            if (validationMessage.getSeverity() == Severity.ERROR) {
                                getLog().error(logMessage);
                                totalErrors++;
                            } else if (validationMessage.getSeverity() == Severity.WARN) {
                                getLog().warn(logMessage);
                                totalWarnings++;
                            } else {
                                getLog().info(logMessage);
                            }
                        }
                    }
                } catch (Exception e) {
                    getLog().error("Error linting file " + file + ": " + e.getMessage());
                    if (failOnError) {
                        throw e;
                    }
                }
            }
            
            // Close the linter
            linter.close();
            
            // Summary
            getLog().info("");
            getLog().info("Linting complete:");
            getLog().info("  Errors: " + totalErrors);
            getLog().info("  Warnings: " + totalWarnings);
            
            // Check if we should fail the build
            if (totalErrors > 0 && failOnError) {
                throw new MojoFailureException("AsciiDoc linting failed with " + totalErrors + " error(s)");
            }
            
        } catch (IOException e) {
            throw new MojoExecutionException("Error executing AsciiDoc linter", e);
        }
    }
}