package com.dataliquid.maven.asciidoc.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.SafeMode;

import com.dataliquid.maven.asciidoc.parser.FrontMatterParser;
import com.dataliquid.maven.asciidoc.util.FilePatternMatcher;

/**
 * Abstract base class for all AsciiDoc-related Mojos. Provides common
 * functionality following DRY principles.
 */
public abstract class AbstractAsciiDocMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(property = "asciidoc.sourceDirectory", defaultValue = "${project.basedir}/src/docs/asciidoc")
    protected File sourceDirectory;

    @Parameter(property = "asciidoc.skip", defaultValue = "false")
    protected boolean skip;

    @Parameter(property = "asciidoc.includes", defaultValue = "**/*.adoc")
    protected String[] includes;

    @Parameter(property = "asciidoc.excludes")
    protected String[] excludes;

    @Parameter(property = "asciidoc.safeMode", defaultValue = "SAFE")
    protected String safeMode;

    private Asciidoctor asciidoctor;
    private FrontMatterParser frontMatterParser;

    /**
     * Template method that subclasses must implement to process the found files.
     */
    protected abstract void processFiles(List<Path> files) throws MojoExecutionException, MojoFailureException;

    /**
     * Get the name of this mojo for logging purposes.
     */
    protected abstract String getMojoName();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (shouldSkip()) {
            return;
        }

        getLog().info("Starting " + getMojoName() + "...");

        if (!validateSourceDirectory()) {
            return;
        }

        try {
            List<Path> files = findAsciiDocFiles();
            getLog().info("Found " + files.size() + " AsciiDoc files");

            if (!files.isEmpty()) {
                processFiles(files);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error during " + getMojoName(), e);
        }
    }

    /**
     * Check if this mojo should be skipped.
     */
    protected boolean shouldSkip() {
        if (skip) {
            getLog().info("Skipping " + getMojoName());
            return true;
        }
        return false;
    }

    /**
     * Validate that the source directory exists.
     */
    protected boolean validateSourceDirectory() {
        if (!sourceDirectory.exists()) {
            getLog().warn("Source directory does not exist: " + sourceDirectory);
            return false;
        }
        return true;
    }

    /**
     * Find all AsciiDoc files based on includes/excludes patterns.
     */
    protected List<Path> findAsciiDocFiles() throws IOException {
        FilePatternMatcher matcher = new FilePatternMatcher(sourceDirectory, includes, excludes);
        return matcher.getMatchedFiles().stream().map(File::toPath).collect(Collectors.toList());
    }

    /**
     * Get or create the Asciidoctor instance.
     */
    protected Asciidoctor getAsciidoctor() {
        if (asciidoctor == null) {
            asciidoctor = Asciidoctor.Factory.create();
        }
        return asciidoctor;
    }

    /**
     * Get or create the FrontMatterParser instance.
     */
    protected FrontMatterParser getFrontMatterParser() {
        if (frontMatterParser == null) {
            frontMatterParser = new FrontMatterParser(getLog());
        }
        return frontMatterParser;
    }

    /**
     * Get the configured SafeMode. Supports: UNSAFE, SAFE, SERVER, SECURE Default
     * is SAFE for security reasons.
     */
    protected SafeMode getSafeMode() throws MojoExecutionException {
        try {
            return SafeMode.valueOf(safeMode.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(
                    "Invalid safeMode value: " + safeMode + ". Valid values are: UNSAFE, SAFE, SERVER, SECURE", e);
        }
    }
}