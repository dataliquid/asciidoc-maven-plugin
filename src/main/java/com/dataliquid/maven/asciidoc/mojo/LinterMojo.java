package com.dataliquid.maven.asciidoc.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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

/**
 * Goal to lint AsciiDoc files using asciidoc-linter.
 */
@Mojo(name = "lint")
public class LinterMojo extends AbstractAsciiDocMojo {

    private static final String TRUE_VALUE = "true";

    @Parameter(property = "asciidoc.linter.ruleFile", required = true)
    private File ruleFile;

    @Parameter(property = "asciidoc.linter.failOnError", defaultValue = TRUE_VALUE)
    private boolean failOnError;

    @Parameter(property = "asciidoc.linter.consoleOutputFormat", defaultValue = "enhanced")
    private String consoleOutputFormat;

    @Parameter(property = "asciidoc.linter.useColors", defaultValue = TRUE_VALUE)
    private boolean useColors;

    @Parameter(property = "asciidoc.linter.contextLines", defaultValue = "2")
    private int contextLines;

    @Parameter(property = "asciidoc.linter.showSuggestions", defaultValue = TRUE_VALUE)
    private boolean showSuggestions;

    @Parameter(property = "asciidoc.linter.highlightErrors", defaultValue = TRUE_VALUE)
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
        if (getLog().isInfoEnabled()) {
            getLog().info("Found " + adocFiles.size() + " AsciiDoc files to lint");
        }

        try {
            // Create linter instance
            Linter linter = new Linter();

            // Load configuration from rule file
            if (!ruleFile.exists()) {
                throw new MojoExecutionException("Rule file not found: " + ruleFile.getAbsolutePath());
            }

            if (getLog().isInfoEnabled()) {
                getLog().info("Loading linter configuration from: " + ruleFile.getAbsolutePath());
            }
            ConfigurationLoader configLoader = new ConfigurationLoader();
            LinterConfiguration linterConfiguration = configLoader.loadConfiguration(ruleFile.toPath());

            // Load or create output configuration
            OutputConfiguration outputConfig = createOutputConfiguration();
            MavenReportFormatter formatter = new MavenReportFormatter(outputConfig, getLog());

            // Lint each file
            for (Path file : adocFiles) {
                if (getLog().isInfoEnabled()) {
                    getLog().info("Linting: " + file);
                }
                ValidationResult result = linter.validateFile(file, linterConfiguration);

                // Process results with enhanced formatter
                if (!result.getMessages().isEmpty()) {
                    formatter.format(result);
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

        OutputFormat format = OutputFormat.valueOf(consoleOutputFormat.toUpperCase(Locale.ROOT));
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
}