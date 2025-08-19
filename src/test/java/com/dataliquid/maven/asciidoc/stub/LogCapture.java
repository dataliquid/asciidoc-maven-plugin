package com.dataliquid.maven.asciidoc.stub;

import org.apache.maven.plugin.logging.Log;

/**
 * Test utility that captures all log output into a StringBuilder.
 */
public class LogCapture implements Log {
    private final StringBuilder logOutput = new StringBuilder();
    private final boolean debugEnabled;
    
    public LogCapture() {
        this(false);
    }
    
    public LogCapture(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }
    
    public String getCapturedOutput() {
        return logOutput.toString();
    }
    
    public void clear() {
        logOutput.setLength(0);
    }
    
    @Override
    public boolean isDebugEnabled() { 
        return debugEnabled; 
    }
    
    @Override
    public void debug(CharSequence content) { 
        if (debugEnabled) {
            logOutput.append("[DEBUG] ").append(content).append("\n");
        }
    }
    
    @Override
    public void debug(CharSequence content, Throwable error) { 
        if (debugEnabled) {
            logOutput.append("[DEBUG] ").append(content).append("\n");
            if (error != null) {
                logOutput.append(error.getMessage()).append("\n");
            }
        }
    }
    
    @Override
    public void debug(Throwable error) { 
        if (debugEnabled && error != null) {
            logOutput.append("[DEBUG] ").append(error.getMessage()).append("\n");
        }
    }
    
    @Override
    public boolean isInfoEnabled() { 
        return true; 
    }
    
    @Override
    public void info(CharSequence content) { 
        logOutput.append("[INFO] ").append(content).append("\n");
    }
    
    @Override
    public void info(CharSequence content, Throwable error) { 
        logOutput.append("[INFO] ").append(content).append("\n");
        if (error != null) {
            logOutput.append(error.getMessage()).append("\n");
        }
    }
    
    @Override
    public void info(Throwable error) { 
        if (error != null) {
            logOutput.append("[INFO] ").append(error.getMessage()).append("\n");
        }
    }
    
    @Override
    public boolean isWarnEnabled() { 
        return true; 
    }
    
    @Override
    public void warn(CharSequence content) { 
        logOutput.append("[WARN] ").append(content).append("\n");
    }
    
    @Override
    public void warn(CharSequence content, Throwable error) { 
        logOutput.append("[WARN] ").append(content).append("\n");
        if (error != null) {
            logOutput.append(error.getMessage()).append("\n");
        }
    }
    
    @Override
    public void warn(Throwable error) { 
        if (error != null) {
            logOutput.append("[WARN] ").append(error.getMessage()).append("\n");
        }
    }
    
    @Override
    public boolean isErrorEnabled() { 
        return true; 
    }
    
    @Override
    public void error(CharSequence content) { 
        logOutput.append("[ERROR] ").append(content).append("\n");
    }
    
    @Override
    public void error(CharSequence content, Throwable error) { 
        logOutput.append("[ERROR] ").append(content).append("\n");
        if (error != null) {
            logOutput.append("[ERROR] ").append(error.getMessage()).append("\n");
        }
    }
    
    @Override
    public void error(Throwable error) { 
        if (error != null) {
            logOutput.append("[ERROR] ").append(error.getMessage()).append("\n");
        }
    }
}