package com.dataliquid.maven.asciidoc.mojo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.skyscreamer.jsonassert.Customization;

public class ValidateMojoTest extends AbstractMojoTest<ValidateMojo> {

    @Override
    protected ValidateMojo createMojo() {
        return new ValidateMojo();
    }

    @Test
    public void testMetadataExport() throws Exception {
        // Given
        Path testResourceDir = Path.of("src/test/resources/functional/validate/export");
        File testSourceDir = testResourceDir.toFile();
        File exportFile = new File(outputDir, "metadata-export.json");
        File expectedFile = new File(testResourceDir.toFile(), "expected.json");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "metadataExportFile", exportFile);
        setField(mojo, "includeAttributes", false); // Don't include all attributes for cleaner test

        // When
        mojo.execute();

        // Then
        assertTrue(exportFile.exists(), "Export file should exist");

        // Compare JSON with expected, ignoring timestamp and dynamic attributes
        String expected = Files.readString(expectedFile.toPath());
        String actual = Files.readString(exportFile.toPath());

        // Use JSONAssert with custom comparator to ignore timestamp
        JSONAssert
                .assertEquals(expected, actual,
                        new CustomComparator(JSONCompareMode.LENIENT, new Customization("timestamp", (o1, o2) -> true),
                                new Customization("documents[*].metadata.attributes", (o1, o2) -> true)));
    }

    @Test
    public void testSchemaValidation() throws Exception {
        // Given
        Path testResourceDir = Path.of("src/test/resources/functional/validate/schema-validation");
        File testSourceDir = testResourceDir.toFile();
        File schemaFile = new File(testSourceDir, "schema.json");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "schemaFile", schemaFile);
        setField(mojo, "schemaVersion", "V202012");
        setField(mojo, "includeAttributes", false);
        setField(mojo, "failOnError", true);

        // When
        mojo.execute();

        // Then - should complete without exceptions
        // Schema validation happens internally
        // If validation fails, it would throw MojoFailureException (when
        // failOnError=true)
    }

    @Test
    public void testSchemaValidationFailure() throws Exception {
        // Given
        Path testResourceDir = Path.of("src/test/resources/functional/validate/schema-validation-failure");
        File testSourceDir = testResourceDir.toFile();
        File schemaFile = new File(testSourceDir, "schema.json");

        setField(mojo, "sourceDirectory", testSourceDir);
        setField(mojo, "schemaFile", schemaFile);
        setField(mojo, "schemaVersion", "V202012");
        setField(mojo, "includeAttributes", false);
        setField(mojo, "failOnError", true); // Should fail on validation errors

        // When & Then
        assertThrows(MojoFailureException.class, () -> mojo.execute(),
                "Should throw MojoFailureException when validation fails");
    }

}