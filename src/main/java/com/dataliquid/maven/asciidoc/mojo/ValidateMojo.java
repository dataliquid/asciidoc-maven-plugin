package com.dataliquid.maven.asciidoc.mojo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.asciidoctor.Attributes;
import org.asciidoctor.AttributesBuilder;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;

import com.dataliquid.maven.asciidoc.model.ValidationError;
import com.dataliquid.maven.asciidoc.util.MetadataCollector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

@Mojo(name = "validate")
public class ValidateMojo extends AbstractAsciiDocMojo {

    @Parameter(property = "asciidoc.schemaVersion", defaultValue = "V7")
    private String schemaVersion;

    @Parameter(property = "asciidoc.failOnError", defaultValue = "true")
    private boolean failOnError;

    @Parameter(property = "asciidoc.failFast", defaultValue = "false")
    private boolean failFast;

    @Parameter(property = "asciidoc.schemaFile")
    private File schemaFile;

    @Parameter(property = "asciidoc.includeAttributes", defaultValue = "true")
    private boolean includeAttributes;

    @Parameter(property = "asciidoc.metadataExportFile")
    private File metadataExportFile;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MetadataCollector metadataCollector = new MetadataCollector();

    @Override
    protected String getMojoName() {
        return "AsciiDoc validation";
    }

    @Override
    protected void processFiles(List<Path> adocFiles) throws MojoExecutionException, MojoFailureException {
        List<ValidationError> allErrors = new ArrayList<>();

        // Process all files: collect metadata
        for (Path adocFile : adocFiles) {
            try {
                String relativePath = sourceDirectory.toPath().relativize(adocFile).toString();
                getLog().info("Processing file: " + relativePath);

                // Collect all metadata (front matter + attributes)
                Map<String, Object> allMetadata = collectAllMetadata(adocFile);
                metadataCollector.addDocument(relativePath, allMetadata);

                // Log collected metadata in debug mode
                if (getLog().isDebugEnabled()) {
                    try {
                        String prettyJson = objectMapper
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(allMetadata);
                        getLog().debug("Collected metadata for " + relativePath + ":\n" + prettyJson);
                    } catch (Exception e) {
                        getLog().debug("Failed to serialize metadata for logging: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                allErrors.add(new ValidationError(adocFile, "IO_ERROR", "Failed to read file: " + e.getMessage()));
            }
        }

        // Export metadata if configured
        if (metadataExportFile != null && metadataCollector.size() > 0) {
            try {
                exportMetadata();
            } catch (IOException e) {
                allErrors
                        .add(new ValidationError(Paths.get("EXPORT"), "EXPORT_ERROR",
                                "Failed to export metadata: " + e.getMessage()));
            }
        }

        // Validate against schema if configured
        if (schemaFile != null && metadataCollector.size() > 0) {
            List<ValidationError> validationErrors = validate();
            allErrors.addAll(validationErrors);
        }

        if (!allErrors.isEmpty()) {
            reportErrors(allErrors);

            if (failOnError) {
                throw new MojoFailureException("Validation failed with " + allErrors.size() + " error(s)");
            }
        } else {
            getLog().info("All files validated successfully!");
        }
    }

    private JsonSchema loadSchema(File schemaFileToLoad) throws IOException, MojoExecutionException {
        try {
            // Determine schema version
            SpecVersion.VersionFlag versionFlag = getSchemaVersion();

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(versionFlag);

            // Load schema from file
            String schemaContent = Files.readString(schemaFileToLoad.toPath());
            JsonNode schemaNode = objectMapper.readTree(schemaContent);

            return factory.getSchema(schemaNode);

        } catch (Exception e) {
            throw new MojoExecutionException("Failed to load schema from: " + schemaFileToLoad, e);
        }
    }

    private SpecVersion.VersionFlag getSchemaVersion() throws MojoExecutionException {
        return switch (schemaVersion.toUpperCase()) {
        case "V4" -> SpecVersion.VersionFlag.V4;
        case "V6" -> SpecVersion.VersionFlag.V6;
        case "V7" -> SpecVersion.VersionFlag.V7;
        case "V201909" -> SpecVersion.VersionFlag.V201909;
        case "V202012" -> SpecVersion.VersionFlag.V202012;
        default -> throw new MojoExecutionException(
                "Unsupported schema version: " + schemaVersion + ". Supported versions: V4, V6, V7, V201909, V202012");
        };
    }

    private Map<String, Object> collectAllMetadata(Path adocFile) throws IOException {
        Map<String, Object> metadata = new HashMap<>();

        String content = Files.readString(adocFile);

        // Use AsciidoctorJ to parse the document
        Map<String, Object> allAttributes = new HashMap<>();

        // Use temp directory for diagrams during validation to avoid polluting project
        File tempImagesDir = new File(System.getProperty("java.io.tmpdir"), "asciidoc-validate-images");
        tempImagesDir.mkdirs();
        allAttributes.put("imagesoutdir", tempImagesDir.getAbsolutePath());

        AttributesBuilder attrBuilder = Attributes.builder();
        for (Map.Entry<String, Object> entry : allAttributes.entrySet()) {
            attrBuilder.attribute(entry.getKey(), entry.getValue());
        }
        Attributes documentAttributes = attrBuilder.skipFrontMatter(true).build();

        Options options = Options.builder().safe(SafeMode.UNSAFE).attributes(documentAttributes).build();

        Document document = getAsciidoctor().load(content, options);

        // Add file metadata
        metadata.put("_file", sourceDirectory.toPath().relativize(adocFile).toString());
        metadata.put("_title", document.getTitle());

        // Add front matter (if exists)
        String frontMatter = (String) document.getAttributes().get("front-matter");
        if (frontMatter != null && !frontMatter.trim().isEmpty()) {
            Map<String, Object> frontMatterData = getFrontMatterParser().parse(frontMatter);
            metadata.put("frontmatter", frontMatterData);
        }

        // Add document attributes if requested
        if (includeAttributes) {
            Map<String, Object> attributes = new HashMap<>();
            document.getAttributes().forEach((key, value) -> {
                // Filter out internal attributes
                if (!key.startsWith("asciidoctor-") && !key.startsWith("backend-") && !key.equals("docfile")
                        && !key.equals("docdir") && !key.equals("front-matter") && !key.equals("filetype")) {
                    attributes.put(key, value);
                }
            });
            metadata.put("attributes", attributes);
        }

        return metadata;
    }

    private void reportErrors(List<ValidationError> errors) {
        getLog().error("=== Validation Errors ===");

        // Group errors by file
        Map<Path, List<ValidationError>> errorsByFile = errors
                .stream()
                .collect(Collectors.groupingBy(ValidationError::getFile));

        for (Map.Entry<Path, List<ValidationError>> fileErrorsEntry : errorsByFile.entrySet()) {
            getLog().error("");
            getLog().error("File: " + fileErrorsEntry.getKey());

            for (ValidationError error : fileErrorsEntry.getValue()) {
                getLog().error("  - [" + error.getType() + "] " + error.getMessage());
            }
        }

        getLog().error("");
        getLog().error("Total errors: " + errors.size() + " in " + errorsByFile.size() + " file(s)");
    }

    private List<ValidationError> validate() {
        List<ValidationError> errors = new ArrayList<>();

        try {
            // Load schema
            JsonSchema schema = loadSchema(schemaFile);

            // Convert metadata to JSON
            Map<String, Object> metadataJson = metadataCollector.toJson();
            JsonNode metadataNode = objectMapper.valueToTree(metadataJson);

            // Validate against schema
            Set<ValidationMessage> validationMessages = schema.validate(metadataNode);

            if (!validationMessages.isEmpty()) {
                getLog().info("Validation found " + validationMessages.size() + " issues");
                for (ValidationMessage validationMessage : validationMessages) {
                    errors
                            .add(new ValidationError(Paths.get("VALIDATION"), "SCHEMA_VALIDATION",
                                    validationMessage.getMessage()));
                }
            }

            getLog().info("Validation completed: " + metadataCollector.size() + " documents validated");

        } catch (Exception e) {
            errors
                    .add(new ValidationError(Paths.get("VALIDATION"), "VALIDATION_ERROR",
                            "Failed to validate: " + e.getMessage()));
        }

        return errors;
    }

    private void exportMetadata() throws IOException {
        // Create parent directory if needed
        if (metadataExportFile.getParentFile() != null && !metadataExportFile.getParentFile().exists()) {
            Files.createDirectories(metadataExportFile.getParentFile().toPath());
        }

        // Convert metadata to JSON
        Map<String, Object> metadataJson = metadataCollector.toJson();

        // Write pretty-printed JSON to file
        String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadataJson);

        Files.writeString(metadataExportFile.toPath(), jsonOutput);

        getLog().info("Metadata exported to: " + metadataExportFile.getAbsolutePath());
        getLog().info("Exported metadata for " + metadataCollector.size() + " documents");
    }

}