package com.dataliquid.maven.asciidoc.template;

import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;
import org.apache.maven.plugin.logging.Log;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Detailed error listener for StringTemplate that provides enhanced error
 * diagnostics with context lines and column indicators.
 */
public class DetailedSTErrorListener implements STErrorListener {
    private final String templateSource;
    private final String templateName;
    private final Log log;

    /**
     * Creates a new detailed error listener.
     *
     * @param templateSource The template source code
     * @param templateName   The name of the template (file name or
     *                       "inline-template")
     * @param log            Maven logger instance
     */
    public DetailedSTErrorListener(String templateSource, String templateName, Log log) {
        this.templateSource = templateSource;
        this.templateName = templateName;
        this.log = log;
    }

    @Override
    public void compileTimeError(STMessage msg) {
        logTemplateError(msg, "Compile-time");
    }

    @Override
    public void runTimeError(STMessage msg) {
        logTemplateError(msg, "Runtime");
    }

    @Override
    public void IOError(STMessage msg) {
        if (log.isErrorEnabled()) {
            log.error("StringTemplate IO error in " + templateName + ": " + msg.toString());
        }
    }

    @Override
    public void internalError(STMessage msg) {
        if (log.isErrorEnabled()) {
            log.error("StringTemplate internal error in " + templateName + ": " + msg.toString());
        }
    }

    /**
     * Logs template errors with context showing surrounding lines and error
     * location.
     *
     * @param msg       The error message from StringTemplate
     * @param errorType The type of error (Compile-time or Runtime)
     */
    private void logTemplateError(STMessage msg, String errorType) {
        String errorMsg = msg.toString();
        if (log.isErrorEnabled()) {
            log.error("StringTemplate " + errorType + " error in " + templateName + ": " + errorMsg);
        }

        if (templateSource != null && !templateSource.isEmpty()) {
            String[] lines = templateSource.split("\n");

            // Try to extract line and column from the error message
            int line = -1;
            int charPos = -1;

            // First try to get position from the template if available
            if (msg.self != null && msg.self.impl != null) {
                try {
                    // ST internal structure may have position info
                    if (log.isDebugEnabled()) {
                        log.debug("Template implementation class: " + msg.self.impl.getClass().getName());
                    }
                } catch (Exception e) {
                    // Ignore errors accessing template internals - expected for internal
                    // StringTemplate structures
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to access template internals (expected): " + e.getMessage());
                    }
                }
            }

            // Check if error message contains line:column format
            if (msg.cause != null && msg.cause.getMessage() != null) {
                String causeMsg = msg.cause.getMessage();
                // Parse "line:column:" format (e.g., "13:8: mismatched input...")
                if (causeMsg.matches("\\d+:\\d+:.*")) {
                    String[] parts = causeMsg.split(":");
                    try {
                        line = Integer.parseInt(parts[0]);
                        charPos = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        // Ignore parse errors - error message format may vary
                        if (log.isDebugEnabled()) {
                            log.debug("Could not parse line:column from cause message (expected): " + e.getMessage());
                        }
                    }
                }
            }

            // Try to extract from main error message with different patterns
            if (line == -1 && charPos == -1) {
                // Pattern for "context [anonymous] 14:24 no such template"
                Pattern runtimePattern = Pattern.compile("context \\[[^\\]]+\\] (\\d+):(\\d+)");
                Matcher runtimeMatcher = runtimePattern.matcher(errorMsg);
                if (runtimeMatcher.find()) {
                    try {
                        line = Integer.parseInt(runtimeMatcher.group(1));
                        charPos = Integer.parseInt(runtimeMatcher.group(2));
                    } catch (NumberFormatException e) {
                        // Ignore parse errors - runtime error message format may vary
                        if (log.isDebugEnabled()) {
                            log
                                    .debug("Could not parse line:column from runtime error message (expected): "
                                            + e.getMessage());
                        }
                    }
                } else {
                    // Original pattern for compile errors
                    Pattern pattern = Pattern.compile("(\\d+):(\\d+):");
                    Matcher matcher = pattern.matcher(errorMsg);
                    if (matcher.find()) {
                        try {
                            line = Integer.parseInt(matcher.group(1));
                            charPos = Integer.parseInt(matcher.group(2));
                        } catch (NumberFormatException e) {
                            // Ignore parse errors - compile error message format may vary
                            if (log.isDebugEnabled()) {
                                log
                                        .debug("Could not parse line:column from compile error message (expected): "
                                                + e.getMessage());
                            }
                        }
                    }
                }
            }

            // Show context if we have line information
            if (line > 0 && line <= lines.length) {
                if (log.isErrorEnabled()) {
                    log.error("Template error context in " + templateName + ":");
                }

                // Show 3 lines before and after
                int startLine = Math.max(0, line - 4);
                int endLine = Math.min(lines.length, line + 3);

                // Process error line context - pre-calculate pointer info to avoid object
                // instantiation in loop
                String errorPointerString = null;
                if (charPos > 0) {
                    // Pre-create StringBuilder outside of any loops
                    StringBuilder pointer = new StringBuilder();

                    // Find the error line first
                    for (int i = startLine; i < endLine; i++) {
                        if (i == line - 1) {
                            String lineMarker = " >>> ";
                            String lineNumberPrefix = String.format("%3d%s", i + 1, lineMarker);
                            int prefixLength = lineNumberPrefix.length();

                            // Clear and build the pointer line
                            pointer.setLength(0);
                            // Add spaces for the line number prefix
                            for (int j = 0; j < prefixLength; j++) {
                                pointer.append(' ');
                            }

                            // Add spaces up to the error position in the actual line content
                            // charPos is 1-based, so we need charPos-1 spaces
                            for (int j = 0; j < charPos - 1; j++) {
                                if (j < lines[i].length() && lines[i].charAt(j) == '\t') {
                                    // Preserve tabs for proper alignment
                                    pointer.append('\t');
                                } else {
                                    pointer.append(' ');
                                }
                            }
                            pointer.append("^ Error here");
                            errorPointerString = pointer.toString();
                            break;
                        }
                    }
                }

                for (int i = startLine; i < endLine; i++) {
                    String lineMarker = (i == line - 1) ? " >>> " : "     ";
                    if (log.isErrorEnabled()) {
                        log.error(String.format("%3d%s%s", i + 1, lineMarker, lines[i]));
                    }

                    // Show column indicator for error line
                    if (i == line - 1 && charPos > 0 && errorPointerString != null) {
                        if (log.isErrorEnabled()) {
                            log.error(errorPointerString);
                        }
                    }
                }
            }
        }
    }
}