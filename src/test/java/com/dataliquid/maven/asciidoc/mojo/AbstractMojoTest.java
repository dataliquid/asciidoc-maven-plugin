package com.dataliquid.maven.asciidoc.mojo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

/**
 * Abstract base class for Mojo tests providing common helper methods and setup.
 */
public abstract class AbstractMojoTest<T extends AbstractMojo> {
    
    @TempDir
    protected Path tempDir;
    
    protected T mojo;
    protected File sourceDir;
    protected File outputDir;
    protected File workDir;
    
    /**
     * Create the mojo instance to be tested.
     */
    protected abstract T createMojo();
    
    @BeforeEach
    void setUp() throws Exception {
        mojo = createMojo();
        mojo.setLog(new SystemStreamLog());
        
        sourceDir = tempDir.resolve("src").toFile();
        outputDir = tempDir.resolve("target/generated-docs").toFile();
        workDir = tempDir.resolve("target/work").toFile();
        sourceDir.mkdirs();
        
        configureDefaultMojo(mojo);
    }
    
    /**
     * Configure default values for the mojo.
     * Override this method to change default configuration.
     */
    protected void configureDefaultMojo(T mojo) throws Exception {
        // Common fields in AbstractAsciiDocMojo
        setField(mojo, "sourceDirectory", sourceDir);
        setField(mojo, "project", createMavenProject());
        setField(mojo, "skip", false);
        setField(mojo, "includes", new String[]{"*.adoc"});
        
        // Common fields that might be in specific mojos
        setFieldIfExists(mojo, "outputDirectory", outputDir);
        setFieldIfExists(mojo, "workDirectory", workDir);
        setFieldIfExists(mojo, "failOnNoFiles", false);
        setFieldIfExists(mojo, "enableDiagrams", false);
        setFieldIfExists(mojo, "enableIncremental", false);
        setFieldIfExists(mojo, "attributes", new HashMap<>());
        
        // Template parameters
        setFieldIfExists(mojo, "templateFile", "templates/partial.st");
        setFieldIfExists(mojo, "outputFormat", "html");
    }
    
    /**
     * Set a field value on the mojo, searching the class hierarchy.
     */
    protected void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        if (field == null) {
            throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
        }
        field.setAccessible(true);
        field.set(obj, value);
    }
    
    /**
     * Set a field value if it exists, otherwise ignore.
     */
    protected void setFieldIfExists(Object obj, String fieldName, Object value) throws Exception {
        Field field = findField(obj.getClass(), fieldName);
        if (field != null) {
            field.setAccessible(true);
            field.set(obj, value);
        }
    }
    
    /**
     * Find a field in the class hierarchy.
     */
    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
    
    /**
     * Load a test resource file as a string.
     */
    protected String loadTestResource(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Test resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes()).trim();
        }
    }
    
    /**
     * Load a file from the filesystem as a string.
     */
    protected String loadFile(File file) throws IOException {
        return Files.readString(file.toPath()).trim();
    }
    
    /**
     * Create a default Maven project for testing.
     */
    protected MavenProject createMavenProject() {
        MavenProject project = new MavenProject();
        project.setGroupId("test");
        project.setArtifactId("test");
        project.setVersion("1.0");
        return project;
    }
}