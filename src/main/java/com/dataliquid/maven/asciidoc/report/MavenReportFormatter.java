package com.dataliquid.maven.asciidoc.report;

import java.io.PrintWriter;

import org.apache.maven.plugin.logging.Log;

import com.dataliquid.asciidoc.linter.config.output.OutputConfiguration;
import com.dataliquid.asciidoc.linter.report.ConsoleFormatter;
import com.dataliquid.asciidoc.linter.report.ReportFormatter;
import com.dataliquid.asciidoc.linter.validator.ValidationResult;

/**
 * Maven-specific implementation of ReportFormatter that delegates to the native
 * ConsoleFormatter while routing output through Maven's logging system.
 * <p>
 * This is the CORRECT integration point - implementing the ReportFormatter interface
 * rather than extending ConsoleFormatter. This approach:
 * <ul>
 * <li>Uses the native formatting capabilities without reimplementation</li>
 * <li>Properly bridges to Maven's logging system</li>
 * <li>Maintains all advanced features (underlines, suggestions, context)</li>
 * <li>Avoids reflection and internal API dependencies</li>
 * </ul>
 */
public class MavenReportFormatter implements ReportFormatter {

    private final Log mavenLog;
    private final OutputConfiguration config;
    private final ConsoleFormatter delegate;

    /**
     * Creates a new MavenReportFormatter with the specified configuration.
     *
     * @param config   The output configuration controlling formatting options
     * @param mavenLog The Maven log to route output through
     */
    public MavenReportFormatter(OutputConfiguration config, Log mavenLog) {
        this.mavenLog = mavenLog;
        this.config = config;
        this.delegate = new ConsoleFormatter(config);
    }

    /**
     * Formats the validation result using the native ConsoleFormatter and routes
     * the output through Maven's logging system.
     * <p>
     * This method uses Maven's logging system directly and doesn't require
     * an external writer.
     *
     * @param result The validation result to format
     */
    public void format(ValidationResult result) {
        // Create a MavenLogWriter that bridges to Maven's logging
        boolean stripAnsi = !config.getDisplay().isUseColors() || !MavenLogWriter.supportsAnsiColors();
        MavenLogWriter mavenWriter = new MavenLogWriter(mavenLog, stripAnsi);

        // Delegate all formatting to the native ConsoleFormatter
        // This preserves all features: underlines, context, suggestions, summary
        delegate.format(result, mavenWriter);

        // Ensure all buffered content is written
        mavenWriter.flush();
    }

    /**
     * Formats the validation result using the native ConsoleFormatter and routes
     * the output through Maven's logging system.
     *
     * @param result The validation result to format
     * @param writer The writer to output to (ignored - uses Maven logging instead)
     */
    @Override
    public void format(ValidationResult result, PrintWriter writer) {
        // Delegate to the parameterless version that uses Maven logging
        format(result);
    }

    /**
     * Returns the name of this formatter.
     *
     * @return "maven-console"
     */
    @Override
    public String getName() {
        return "maven-console";
    }
}