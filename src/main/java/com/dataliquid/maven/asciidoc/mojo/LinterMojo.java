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
import com.dataliquid.asciidoc.linter.config.loader.ConfigurationLoader;
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
import com.dataliquid.maven.asciidoc.yaml.YamlAsciiDocProcessor;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;

/**
 * Goal to lint AsciiDoc files using asciidoc-linter.
 */
@Mojo(name = "lint")
public class LinterMojo extends AbstractAsciiDocMojo {

    @Parameter(property = "asciidoc.linter.ruleFile", required = true)
    private File ruleFile;

    @Parameter(property = "asciidoc.linter.failOnError", defaultValue = "true")
    private boolean failOnError;

    @Parameter(property = "asciidoc.linter.consoleOutputFormat", defaultValue = "enhanced")
    private String consoleOutputFormat;

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

            // Lint each file
            for (Path file : adocFiles) {
                String fileName = file.getFileName().toString().toLowerCase();

                if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                    // Process YAML files with embedded AsciiDoc
                    lintYamlFile(file, linter, linterConfiguration, formatter);
                } else {
                    // Process regular AsciiDoc files
                    getLog().info("Linting: " + file);
                    ValidationResult result = linter.validateFile(file, linterConfiguration);

                    // Process results with enhanced formatter
                    if (!result.getMessages().isEmpty()) {
                        formatter.format(result);
                    }
                }
            }

            // Close the linter
            linter.close();
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error during linting", e);
        }
    }

    /**
     * Creates the output configuration based on Maven parameters. Supports
     * predefined formats (enhanced, simple, compact) or custom configuration.
     */
    private OutputConfiguration createOutputConfiguration() throws IOException {
        OutputConfigurationLoader outputLoader = new OutputConfigurationLoader();

        OutputFormat format = OutputFormat.valueOf(consoleOutputFormat.toUpperCase());
        OutputConfiguration baseConfig = outputLoader.loadPredefinedConfiguration(format);
        SummaryConfig summaryConfig = baseConfig.getSummary();
        boolean showLineNumbers = baseConfig.getDisplay().isShowLineNumbers();

        return OutputConfiguration
                .builder()
                .display(DisplayConfig
                        .builder()
                        .contextLines(contextLines)
                        .highlightStyle(highlightErrors ? HighlightStyle.UNDERLINE : HighlightStyle.NONE)
                        .useColors(useColors && MavenLogWriter.supportsAnsiColors())
                        .showLineNumbers(showLineNumbers)
                        .showHeader(false)
                        .build())
                .suggestions(SuggestionsConfig
                        .builder()
                        .enabled(showSuggestions)
                        .maxPerError(maxSuggestionsPerError)
                        .showExamples(showExamples)
                        .build())
                .summary(summaryConfig)
                .build();
    }

    /**
     * Lint a YAML file containing !asciidoc tags
     */
    private void lintYamlFile(Path yamlFile, Linter linter, LinterConfiguration linterConfiguration,
            MavenReportFormatter formatter) throws IOException {
        getLog().info("Linting YAML file: " + yamlFile);

        // Create YamlAsciiDocProcessor instance (without Asciidoctor for extraction
        // only)
        YamlAsciiDocProcessor yamlProcessor = new YamlAsciiDocProcessor(null, null, getLog());

        // Extract AsciiDoc content from YAML
        List<YamlAsciiDocProcessor.ExtractedContent> extractedContents = yamlProcessor.extractAsciiDocContent(yamlFile);

        if (extractedContents.isEmpty()) {
            getLog().debug("No !asciidoc tags found in: " + yamlFile);
            return;
        }

        getLog().info("Found " + extractedContents.size() + " AsciiDoc content blocks in YAML file");

        // Lint each extracted content
        for (YamlAsciiDocProcessor.ExtractedContent extracted : extractedContents) {
            getLog().debug("Linting YAML path: " + extracted.getYamlPath());

            // Validate the content string
            ValidationResult result = linter.validateContent(extracted.getContent(), linterConfiguration);

            // If there are validation messages, log them with YAML context
            if (!result.getMessages().isEmpty()) {
                // Log the YAML path context for this content block
                getLog().info("");
                getLog().info("YAML file: " + yamlFile);
                getLog().info("YAML path: " + extracted.getYamlPath());

                // Format and display the validation results
                formatter.format(result);
            }
        }
    }
}