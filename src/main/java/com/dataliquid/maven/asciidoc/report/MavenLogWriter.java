package com.dataliquid.maven.asciidoc.report;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Pattern;

import org.apache.maven.plugin.logging.Log;

import com.dataliquid.asciidoc.linter.config.common.Severity;

/**
 * A PrintWriter adapter that bridges asciidoc-linter's console output to Maven's logging system.
 * This class intercepts output from the ConsoleFormatter and routes it appropriately
 * to Maven's log levels while preserving formatting like colors and underlines.
 */
public class MavenLogWriter extends PrintWriter {
    
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");
    private static final Pattern ERROR_LINE = Pattern.compile("^\\[ERROR\\]|^ERROR:");
    private static final Pattern WARN_LINE = Pattern.compile("^\\[WARN\\]|^WARNING:");
    private static final Pattern INFO_LINE = Pattern.compile("^\\[INFO\\]");
    
    private final Log mavenLog;
    private final boolean stripAnsi;
    private final StringWriter buffer;
    private Severity currentSeverity = Severity.INFO;
    
    public MavenLogWriter(Log mavenLog, boolean stripAnsi) {
        super(new StringWriter());
        this.mavenLog = mavenLog;
        this.stripAnsi = stripAnsi;
        this.buffer = (StringWriter) out;
    }
    
    /**
     * Sets the current severity level for subsequent output.
     * This helps route messages to the correct Maven log level.
     */
    public void setCurrentSeverity(Severity severity) {
        this.currentSeverity = severity;
    }
    
    @Override
    public void println() {
        flush();
        processLine("");
    }
    
    @Override
    public void println(String line) {
        flush();
        processLine(line);
    }
    
    @Override
    public void println(Object obj) {
        println(String.valueOf(obj));
    }
    
    @Override
    public void flush() {
        String content = buffer.toString();
        if (!content.isEmpty()) {
            buffer.getBuffer().setLength(0);
            if (content.contains("\n")) {
                String[] lines = content.split("\n", -1);
                for (int i = 0; i < lines.length - 1; i++) {
                    processLine(lines[i]);
                }
                if (!lines[lines.length - 1].isEmpty()) {
                    buffer.write(lines[lines.length - 1]);
                }
            } else {
                buffer.write(content);
            }
        }
    }
    
    private void processLine(String line) {
        String processedLine = stripAnsi ? removeAnsiCodes(line) : line;
        
        // For error messages, keep everything as ERROR level
        // The linter outputs full error blocks that should stay together
        if (currentSeverity == Severity.ERROR) {
            mavenLog.error(processedLine);
        } else if (currentSeverity == Severity.WARN) {
            mavenLog.warn(processedLine);
        } else {
            // Only for INFO level, try to detect if this line contains an error marker
            Severity detectedSeverity = detectSeverity(processedLine);
            if (detectedSeverity == Severity.ERROR) {
                mavenLog.error(processedLine);
                currentSeverity = Severity.ERROR; // Switch context to ERROR
            } else if (detectedSeverity == Severity.WARN) {
                mavenLog.warn(processedLine);
                currentSeverity = Severity.WARN; // Switch context to WARN
            } else {
                mavenLog.info(processedLine);
            }
        }
    }
    
    private Severity detectSeverity(String line) {
        if (ERROR_LINE.matcher(line).find()) {
            return Severity.ERROR;
        } else if (WARN_LINE.matcher(line).find()) {
            return Severity.WARN;
        } else if (INFO_LINE.matcher(line).find()) {
            return Severity.INFO;
        }
        return null;
    }
    
    private String removeAnsiCodes(String text) {
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }
    
    /**
     * Checks if the terminal supports ANSI colors.
     * Can be overridden by environment variables or system properties.
     */
    public static boolean supportsAnsiColors() {
        // Check for NO_COLOR environment variable (https://no-color.org/)
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        
        // Check for MAVEN_COLOR property
        String mavenColor = System.getProperty("maven.color", System.getenv("MAVEN_COLOR"));
        if ("false".equalsIgnoreCase(mavenColor)) {
            return false;
        }
        
        // Check if we're in a terminal that supports colors
        String term = System.getenv("TERM");
        if (term == null || "dumb".equals(term)) {
            return false;
        }
        
        // Check if we have a console (not redirected)
        return System.console() != null;
    }
}