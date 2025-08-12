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
import com.dataliquid.asciidoc.linter.config.output.OutputConfiguration;
import com.dataliquid.asciidoc.linter.config.output.OutputConfigurationLoader;
import com.dataliquid.asciidoc.linter.config.output.OutputFormat;
import com.dataliquid.asciidoc.linter.config.output.DisplayConfig;
import com.dataliquid.asciidoc.linter.config.output.SuggestionsConfig;
import com.dataliquid.asciidoc.linter.config.output.SummaryConfig;
import com.dataliquid.asciidoc.linter.config.output.HighlightStyle;
import com.dataliquid.maven.asciidoc.report.MavenReportFormatter;
import com.dataliquid.maven.asciidoc.report.MavenLogWriter;

/**
 * Goal to lint AsciiDoc files using asciidoc-linter.
 */
@Mojo(name = "lint")
public class LinterMojo extends AbstractAsciiDocMojo {

    @Parameter(property = "asciidoc.linter.ruleFile", required = true)
    private File ruleFile;

    @Parameter(property = "asciidoc.linter.failOnError", defaultValue = "true")
    private boolean failOnError;
    
    @Parameter(property = "asciidoc.linter.outputFormat", defaultValue = "enhanced")
    private String outputFormat;
    
    @Parameter(property = "asciidoc.linter.useColors", defaultValue = "true")
    private boolean useColors;
    
    @Parameter(property = "asciidoc.linter.contextLines", defaultValue = "2")
    private int contextLines;
    
    @Parameter(property = "asciidoc.linter.showSuggestions", defaultValue = "true")
    private boolean showSuggestions;
    
    @Parameter(property = "asciidoc.linter.highlightErrors", defaultValue = "true")
    private boolean highlightErrors;
    
    @Parameter(property = "asciidoc.linter.showExamples", defaultValue = "false")
    private boolean showExamples;
    
    @Parameter(property = "asciidoc.linter.maxSuggestionsPerError", defaultValue = "3")
    private int maxSuggestionsPerError;

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
            
            // Load or create output configuration
            OutputConfiguration outputConfig = createOutputConfiguration();
            MavenReportFormatter formatter = new MavenReportFormatter(outputConfig, getLog());
            
            int totalErrors = 0;
            int totalWarnings = 0;
            
            // Lint each file
            for (Path file : adocFiles) {
                getLog().info("Linting: " + file);
                
                try {
                    ValidationResult result = linter.validateFile(file, linterConfiguration);
                    
                    // Process results with enhanced formatter
                    if (!result.getMessages().isEmpty()) {
                        // Use the enhanced formatter for output
                        formatter.format(result);
                        
                        // Count errors and warnings
                        for (ValidationMessage validationMessage : result.getMessages()) {
                            if (validationMessage.getSeverity() == Severity.ERROR) {
                                totalErrors++;
                            } else if (validationMessage.getSeverity() == Severity.WARN) {
                                totalWarnings++;
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
    
    /**
     * Creates the output configuration based on Maven parameters.
     * Supports predefined formats (enhanced, simple, compact) or custom configuration.
     */
    private OutputConfiguration createOutputConfiguration() throws IOException {
        OutputConfigurationLoader outputLoader = new OutputConfigurationLoader();
        
        // Try to load predefined format
        try {
            OutputFormat format = OutputFormat.valueOf(outputFormat.toUpperCase());
            OutputConfiguration config = outputLoader.loadPredefinedConfiguration(format);
            
            // Override with Maven-specific settings
            return customizeOutputConfiguration(config);
        } catch (IllegalArgumentException e) {
            // If not a predefined format, create custom configuration
            return createCustomOutputConfiguration();
        }
    }
    
    /**
     * Customizes the output configuration with Maven parameters.
     */
    private OutputConfiguration customizeOutputConfiguration(OutputConfiguration base) {
        return OutputConfiguration.builder()
            .display(DisplayConfig.builder()
                .contextLines(contextLines)
                .highlightStyle(highlightErrors ? HighlightStyle.UNDERLINE : HighlightStyle.NONE)
                .useColors(useColors && MavenLogWriter.supportsAnsiColors())
                .showLineNumbers(true)
                .showHeader(false) // No header in Maven output
                .build())
            .suggestions(SuggestionsConfig.builder()
                .enabled(showSuggestions)
                .maxPerError(maxSuggestionsPerError)
                .showExamples(showExamples)
                .build())
            .summary(SummaryConfig.builder()
                .showStatistics(true)
                .showMostCommon(true)
                .showFileList(false) // We already show file-by-file
                .build())
            .build();
    }
    
    /**
     * Creates a custom output configuration from scratch.
     */
    private OutputConfiguration createCustomOutputConfiguration() {
        return customizeOutputConfiguration(OutputConfiguration.builder().build());
    }
}