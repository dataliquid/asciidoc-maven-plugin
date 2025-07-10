package com.dataliquid.maven.asciidoc.template;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;
import org.stringtemplate.v4.STGroupString;
import org.stringtemplate.v4.compiler.STException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.InputStream;
import com.dataliquid.maven.asciidoc.util.IndentationUtils;

public class StringTemplateProcessor {
    private final Log log;
    private final String baseDir;
    private final STGroup templateGroup;
    
    // Constructor that supports both file and classpath templates
    public StringTemplateProcessor(String templatePath, Log log) {
        this.log = log;
        
        if (templatePath == null) {
            throw new IllegalArgumentException("Template path cannot be null");
        }
        
        // Check if it's a file path
        File templateFile = new File(templatePath);
        if (templateFile.exists()) {
            // File-based template
            this.baseDir = templateFile.getParent() != null ? templateFile.getParent() : ".";
            
            log.debug("Initializing StringTemplate with file-based template: " + templatePath);
            
            // Load the template file
            if (templatePath.endsWith(".stg")) {
                this.templateGroup = new STGroupFile(templatePath);
                this.templateGroup.delimiterStartChar = '$';
                this.templateGroup.delimiterStopChar = '$';
            } else {
                // For individual template files, create a group from string
                try {
                    String content = Files.readString(Paths.get(templatePath));
                    // Apply smart indentation removal that preserves relative indentation
                    String processedContent = IndentationUtils.removeCommonIndentation(content);
                    
                    // Check if the content already has a template definition
                    if (processedContent.trim().startsWith("partial(")) {
                        // Use the content as-is, it's already a complete template definition
                        this.templateGroup = new STGroupString(processedContent);
                    } else {
                        // Wrap simple content in a template definition
                        String groupContent = "partial(html,attributes,frontMatter,metadata,context) ::= <<\n" + processedContent + "\n>>";
                        this.templateGroup = new STGroupString(groupContent);
                    }
                    this.templateGroup.delimiterStartChar = '$';
                    this.templateGroup.delimiterStopChar = '$';
                    this.templateGroup.setListener(new DetailedSTErrorListener(processedContent, templatePath, log));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load template file: " + templatePath, e);
                }
            }
        } else {
            // Classpath-based template
            // Extract base directory from classpath path
            int lastSlash = templatePath.lastIndexOf('/');
            this.baseDir = lastSlash > 0 ? templatePath.substring(0, lastSlash) : "";
            
            log.debug("Initializing StringTemplate with classpath resource: " + templatePath);
            
            // Load from classpath
            if (templatePath.endsWith(".stg")) {
                this.templateGroup = new STGroupFile(templatePath);
                this.templateGroup.delimiterStartChar = '$';
                this.templateGroup.delimiterStopChar = '$';
            } else {
                // For individual template files from classpath, need to read and convert
                try {
                    InputStream is = getClass().getResourceAsStream("/" + templatePath);
                    if (is == null) {
                        throw new IllegalArgumentException("Template not found in classpath: " + templatePath);
                    }
                    String content = new String(is.readAllBytes());
                    // Apply smart indentation removal that preserves relative indentation
                    String processedContent = IndentationUtils.removeCommonIndentation(content);
                    
                    // Check if the content already has a template definition
                    if (processedContent.trim().startsWith("partial(")) {
                        // Use the content as-is, it's already a complete template definition
                        this.templateGroup = new STGroupString(processedContent);
                    } else {
                        // Wrap simple content in a template definition
                        String groupContent = "partial(html,attributes,frontMatter,metadata,context) ::= <<\n" + processedContent + "\n>>";
                        this.templateGroup = new STGroupString(groupContent);
                    }
                    this.templateGroup.delimiterStartChar = '$';
                    this.templateGroup.delimiterStopChar = '$';
                    this.templateGroup.setListener(new DetailedSTErrorListener(processedContent, templatePath, log));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load template from classpath: " + templatePath, e);
                }
            }
        }
    }
    
    // Constructor for file-based templates (test use)
    public StringTemplateProcessor(File templateDir, Log log) {
        this.log = log;
        this.baseDir = templateDir.getAbsolutePath();
        
        if (templateDir == null || !templateDir.exists()) {
            throw new IllegalArgumentException("Template directory does not exist: " + templateDir);
        }

        log.debug("Initializing StringTemplate with template directory: " + templateDir);
        
        // For directory-based templates, we'll need to load individual templates
        // StringTemplate doesn't have a direct directory resolver
        this.templateGroup = null; // We'll load templates individually in process()
    }

    public String process(String templateName, DocumentContext context) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }

        log.debug("Processing template: " + templateName);
        
        try {
            ST template;
            String templateContent = null;
            
            if (templateGroup != null) {
                // Get template from group - always use "partial" as the template name
                template = templateGroup.getInstanceOf("partial");
            } else {
                // Load template from file
                Path templatePath = Paths.get(baseDir, templateName);
                templateContent = Files.readString(templatePath);
                // Apply smart indentation removal that preserves relative indentation
                String processedContent = IndentationUtils.removeCommonIndentation(templateContent);
                template = new ST(processedContent, '$', '$');
                
                // Add error listener for file-based templates
                STGroup group = new STGroup();
                group.setListener(new DetailedSTErrorListener(processedContent, templateName, log));
                template.groupThatCreatedThisInstance = group;
            }
            
            if (template == null) {
                throw new IllegalArgumentException("Template not found: " + templateName);
            }
            
            // Add template data
            template.add("html", context.getHtml());
            template.add("attributes", context.getAttributes());
            template.add("frontMatter", context.getFrontMatter());
            template.add("metadata", context.getMetadata());
            template.add("context", context);
            
            return template.render();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + templateName, e);
        } catch (STException e) {
            log.error("Template syntax error in: " + templateName, e);
            throw new RuntimeException("Template processing failed for " + templateName + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing template: " + templateName, e);
            throw new RuntimeException("Template processing failed: " + e.getMessage(), e);
        }
    }
    
    public String processInline(String templateContent, DocumentContext context) {
        if (templateContent == null || templateContent.trim().isEmpty()) {
            throw new IllegalArgumentException("Template content cannot be null or empty");
        }
        
        log.debug("Processing inline template");
        
        // Apply smart indentation removal that preserves relative indentation
        String processedContent = IndentationUtils.removeCommonIndentation(templateContent);
        
        try {
            // Create STGroup with error listener BEFORE creating template
            STGroup group = new STGroup('$', '$');
            group.setListener(new DetailedSTErrorListener(processedContent, "inline-template", log));
            
            // Define template in the group with parameters - this will trigger compile-time errors if syntax is invalid
            group.defineTemplate("inline", "html,attributes,frontMatter,metadata,context", processedContent);
            ST template = group.getInstanceOf("inline");
            
            if (template == null) {
                throw new IllegalStateException("Failed to create template instance");
            }
            
            // Add template data
            template.add("html", context.getHtml());
            template.add("attributes", context.getAttributes());
            template.add("frontMatter", context.getFrontMatter());
            template.add("metadata", context.getMetadata());
            template.add("context", context);
            
            return template.render();
        } catch (STException e) {
            log.error("Template syntax error in inline template", e);
            throw new RuntimeException("Inline template processing failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error processing inline template", e);
            throw new RuntimeException("Inline template processing failed: " + e.getMessage(), e);
        }
    }
    
}