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
 * 
 * This is the CORRECT integration point - implementing the ReportFormatter interface
 * rather than extending ConsoleFormatter. This approach:
 * - Uses the native formatting capabilities without reimplementation
 * - Properly bridges to Maven's logging system
 * - Maintains all advanced features (underlines, suggestions, context)
 * - Avoids reflection and internal API dependencies
 */
public class MavenReportFormatter implements ReportFormatter {
    
    private final Log mavenLog;
    private final OutputConfiguration config;
    private final ConsoleFormatter delegate;
    
    /**
     * Creates a new MavenReportFormatter with the specified configuration.
     * 
     * @param config The output configuration controlling formatting options
     * @param mavenLog The Maven log to route output through
     */
    public MavenReportFormatter(OutputConfiguration config, Log mavenLog) {
        this.mavenLog = mavenLog;
        this.config = config;
        this.delegate = new ConsoleFormatter(config);
    }
    
    /**
     * Formats the validation result using the native ConsoleFormatter
     * and routes the output through Maven's logging system.
     * 
     * @param result The validation result to format
     * @param writer The writer to output to (will be wrapped/replaced)
     */
    @Override
    public void format(ValidationResult result, PrintWriter writer) {
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
     * Returns the name of this formatter.
     * 
     * @return "maven-console"
     */
    @Override
    public String getName() {
        return "maven-console";
    }
}