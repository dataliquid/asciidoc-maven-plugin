package com.dataliquid.maven.asciidoc.mojo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.OptionsBuilder;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;

import com.dataliquid.maven.asciidoc.util.IncrementalBuildManager;
import com.dataliquid.maven.asciidoc.template.DocumentContext;
import com.dataliquid.maven.asciidoc.template.StringTemplateProcessor;
import com.dataliquid.maven.asciidoc.yaml.YamlAsciiDocProcessor;

@Mojo(name = "render")
public class RenderMojo extends AbstractAsciiDocMojo {

    @Parameter(property = "asciidoc.workDirectory", defaultValue = "${project.build.directory}/asciidoc-work")
    private File workDirectory;

    @Parameter(property = "asciidoc.outputDirectory", defaultValue = "${project.build.directory}/generated-docs")
    private File outputDirectory;

    @Parameter(property = "asciidoc.templateDir")
    private File templateDir;

    @Parameter(property = "asciidoc.failOnNoFiles", defaultValue = "false")
    private boolean failOnNoFiles;

    @Parameter(property = "asciidoc.attributes")
    private Map<String, Object> attributes = new HashMap<>();

    @Parameter(property = "asciidoc.enableDiagrams", defaultValue = "true")
    private boolean enableDiagrams;

    @Parameter(property = "asciidoc.diagramFormat", defaultValue = "svg")
    private String diagramFormat;

    @Parameter(property = "asciidoc.enableIncremental", defaultValue = "true")
    private boolean enableIncremental;

    @Parameter(property = "asciidoc.templateFile", defaultValue = "templates/partial.st")
    private String templateFile;

    @Parameter(property = "asciidoc.template")
    private String template;

    @Parameter(property = "asciidoc.outputFormat", defaultValue = "html")
    private String outputFormat;

    @Override
    protected String getMojoName() {
        return "AsciiDoc processing";
    }

    @Override
    protected void processFiles(List<Path> adocFiles) throws MojoExecutionException, MojoFailureException {
        if (adocFiles.isEmpty() && failOnNoFiles) {
            throw new MojoFailureException("No AsciiDoc files found matching the pattern");
        }

        try {
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            if (!workDirectory.exists()) {
                workDirectory.mkdirs();
            }

            if (enableDiagrams) {
                getLog().info("Diagram support enabled (format: " + diagramFormat + ")");
                getAsciidoctor().requireLibrary("asciidoctor-diagram");
            }

            IncrementalBuildManager incrementalManager = null;
            if (enableIncremental) {
                try {
                    incrementalManager = new IncrementalBuildManager(workDirectory, getLog());
                    getLog().info("Incremental build enabled");
                } catch (Exception e) {
                    getLog().warn("Failed to initialize incremental build manager, falling back to full build", e);
                }
            }

            int skippedCount = 0;

            for (Path adocFile : adocFiles) {
                Path relativePath = sourceDirectory.toPath().relativize(adocFile);
                Path outputPath = outputDirectory
                        .toPath()
                        .resolve(relativePath.toString().replaceAll("\\.adoc$", ".html"));

                boolean shouldProcess = true;
                if (incrementalManager != null) {
                    if (!incrementalManager.needsRegeneration(adocFile, outputPath)) {
                        getLog().debug("Skipping unchanged file: " + adocFile);
                        skippedCount++;
                        shouldProcess = false;
                    }
                }

                if (shouldProcess) {
                    boolean success = processFile(adocFile);
                    if (success && incrementalManager != null) {
                        incrementalManager.updateHash(adocFile);
                    }
                }
            }

            if (incrementalManager != null) {
                Map<String, Path> currentFiles = adocFiles
                        .stream()
                        .collect(Collectors.toMap(Path::toString, path -> path));
                incrementalManager.removeStaleEntries(currentFiles);
                incrementalManager.saveHashCache();

                if (skippedCount > 0) {
                    getLog().info("Skipped " + skippedCount + " unchanged files");
                }
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error processing AsciiDoc files", e);
        }
    }

    private boolean processFile(Path file) {
        try {
            getLog().info("Processing: " + file);

            String fileName = file.getFileName().toString().toLowerCase();
            String processedContent;

            // Check if this is a YAML file
            if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
                processedContent = processYamlFile(file);
            } else {
                processedContent = processAsciiDocFile(file);
            }

            if (processedContent == null) {
                return false;
            }

            // Write output file for both YAML and AsciiDoc
            writeOutputFile(file, processedContent);
            return true;

        } catch (Exception e) {
            getLog().error("Error processing file: " + file, e);
            return false;
        }
    }

    private String processYamlFile(Path yamlFile) throws IOException, MojoExecutionException {
        getLog().info("Processing YAML file with AsciiDoc content: " + yamlFile);
        Options options = createAsciidoctorOptions();
        YamlAsciiDocProcessor yamlProcessor = new YamlAsciiDocProcessor(getAsciidoctor(), options, getLog());
        return yamlProcessor.processYamlFile(yamlFile);
    }

    private String processAsciiDocFile(Path adocFile) throws IOException, MojoExecutionException {
        String content = Files.readString(adocFile);

        // Convert AsciiDoc to HTML
        String generatedHtml = convertAsciiDocToHtml(content, adocFile);
        if (generatedHtml == null) {
            return null;
        }

        // Collect metadata for template processing
        Map<String, Object> metadata = collectAllMetadata(adocFile);

        // Process through template
        return processWithTemplate(generatedHtml, metadata);
    }

    private Options createAsciidoctorOptions() throws MojoExecutionException {
        Map<String, Object> allAttributes = new HashMap<>(attributes);

        if (enableDiagrams) {
            allAttributes.put("diagram-format", diagramFormat);
            // Set imagesoutdir to workDirectory to avoid generating diagrams in project
            // root
            File imagesOutDir = new File(workDirectory, "images");
            imagesOutDir.mkdirs();
            allAttributes.put("imagesoutdir", imagesOutDir.getAbsolutePath());
            allAttributes.put("diagram-cachedir", new File(workDirectory, "diagram-cache").getAbsolutePath());
        }

        // Enable AsciidoctorJ's built-in front matter handling
        Attributes documentAttributes = Attributes.builder().attributes(allAttributes).skipFrontMatter(true).build();

        OptionsBuilder optionsBuilder = Options
                .builder()
                .safe(getSafeMode())
                .mkDirs(true)
                .attributes(documentAttributes);

        if (templateDir != null && templateDir.exists()) {
            optionsBuilder.templateDirs(templateDir);
        }

        return optionsBuilder.build();
    }

    private String convertAsciiDocToHtml(String content, Path adocFile) throws IOException, MojoExecutionException {
        Options options = createAsciidoctorOptions();

        // Convert to HTML using options with custom attributes
        String generatedHtml = getAsciidoctor().convert(content, options);

        if (generatedHtml == null || generatedHtml.trim().isEmpty()) {
            getLog().error("Failed to convert " + adocFile + " - AsciidoctorJ returned null or empty content");
            return null;
        }

        return generatedHtml;
    }

    private String processWithTemplate(String generatedHtml, Map<String, Object> metadata) {
        DocumentContext context = createDocumentContext(generatedHtml, metadata);

        if (hasInlineTemplate()) {
            if (templateFile != null) {
                getLog().warn("Both template (inline) and templateFile are configured. Using inline template.");
            }
            return processInlineTemplate(context);
        }

        return processFileTemplate(context);
    }

    private DocumentContext createDocumentContext(String generatedHtml, Map<String, Object> metadata) {
        @SuppressWarnings("unchecked")
        Map<String, Object> frontMatterData = (Map<String, Object>) metadata
                .getOrDefault("frontmatter", new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Object> docAttributes = (Map<String, Object>) metadata.getOrDefault("attributes", new HashMap<>());

        return new DocumentContext(generatedHtml, docAttributes, frontMatterData, metadata);
    }

    private boolean hasInlineTemplate() {
        return template != null;
    }

    private String processInlineTemplate(DocumentContext context) {
        StringTemplateProcessor processor = new StringTemplateProcessor(templateFile, getLog());
        return processor.processInline(template, context);
    }

    private String processFileTemplate(DocumentContext context) {
        StringTemplateProcessor processor = new StringTemplateProcessor(templateFile, getLog());
        return processor.process(templateFile, context);
    }

    private void writeOutputFile(Path inputFile, String content) throws IOException {
        Path absoluteRelativePath = sourceDirectory.toPath().toAbsolutePath().relativize(inputFile.toAbsolutePath());
        getLog().debug("Source dir: " + sourceDirectory.toPath().toAbsolutePath());
        getLog().debug("Input file: " + inputFile.toAbsolutePath());
        getLog().debug("Relative path: " + absoluteRelativePath);

        String outputFileName = determineOutputFileName(inputFile, absoluteRelativePath);
        getLog().debug("Output filename: " + outputFileName);

        Path outputPath = outputDirectory.toPath().resolve(outputFileName);
        Files.createDirectories(outputPath.getParent());
        Files.writeString(outputPath, content);

        getLog().info("Generated: " + outputPath);
    }

    private String determineOutputFileName(Path inputFile, Path relativePath) {
        String fileName = inputFile.getFileName().toString().toLowerCase();

        // For YAML files, keep the original extension
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return relativePath.toString();
        }

        // For AsciiDoc files, replace extension with outputFormat
        String extension = "." + outputFormat;
        if (relativePath.toString().isEmpty()) {
            // File is directly in source directory
            return inputFile.getFileName().toString().replaceAll("\\.adoc$", extension);
        } else {
            return relativePath.toString().replaceAll("\\.adoc$", extension);
        }
    }

    private Map<String, Object> collectAllMetadata(Path adocFile) throws IOException, MojoExecutionException {
        Map<String, Object> metadata = new HashMap<>();

        String content = Files.readString(adocFile);

        // Use AsciidoctorJ to parse the document
        Map<String, Object> allAttributes = new HashMap<>(attributes);

        // Prevent diagram generation in project root (same as in convertAsciiDocToHtml)
        if (enableDiagrams) {
            File imagesOutDir = new File(workDirectory, "images");
            imagesOutDir.mkdirs();
            allAttributes.put("imagesoutdir", imagesOutDir.getAbsolutePath());
            allAttributes.put("diagram-format", diagramFormat);
            allAttributes.put("diagram-cachedir", new File(workDirectory, "diagram-cache").getAbsolutePath());
        }

        Attributes documentAttributes = Attributes.builder().attributes(allAttributes).skipFrontMatter(true).build();

        Options options = Options.builder().safe(getSafeMode()).attributes(documentAttributes).build();

        Document document = getAsciidoctor().load(content, options);

        // Add file metadata
        metadata.put("_file", sourceDirectory.toPath().relativize(adocFile).toString());
        metadata.put("_title", document != null ? document.getTitle() : null);

        // Add front matter (if exists)
        if (document != null) {
            String frontMatter = (String) document.getAttributes().get("front-matter");
            if (frontMatter != null && !frontMatter.trim().isEmpty()) {
                Map<String, Object> frontMatterData = getFrontMatterParser().parse(frontMatter);
                metadata.put("frontmatter", frontMatterData);
            }

            // Add document attributes
            Map<String, Object> attributes = new HashMap<>();
            document.getAttributes().forEach((key, value) -> {
                // Filter out internal attributes
                if (!key.startsWith("asciidoctor-") && !key.startsWith("backend-") && !key.equals("docfile")
                        && !key.equals("docdir") && !key.equals("front-matter") && !key.equals("filetype")) {
                    attributes.put(key, value);
                }
            });
            metadata.put("attributes", attributes);
        } else {
            metadata.put("frontmatter", new HashMap<>());
            metadata.put("attributes", new HashMap<>());
        }

        return metadata;
    }
}