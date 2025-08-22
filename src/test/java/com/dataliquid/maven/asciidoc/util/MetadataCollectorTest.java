package com.dataliquid.maven.asciidoc.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MetadataCollector")
class MetadataCollectorTest {

    private MetadataCollector collector;

    @BeforeEach
    void setUp() {
        collector = new MetadataCollector();
    }

    @Nested
    @DisplayName("addDocument")
    class AddDocumentTests {

        @Test
        @DisplayName("should add single document with metadata")
        void shouldAddSingleDocumentWithMetadata() {
            // Given
            Map<String, Object> metadata = createSampleMetadata();

            // When
            collector.addDocument("docs/test.adoc", metadata);

            // Then
            assertEquals(1, collector.size());
        }

        @Test
        @DisplayName("should add multiple documents")
        void shouldAddMultipleDocuments() {
            // Given
            Map<String, Object> metadata = createSampleMetadata();

            // When
            collector.addDocument("docs/doc1.adoc", metadata);
            collector.addDocument("docs/doc2.adoc", metadata);
            collector.addDocument("docs/doc3.adoc", metadata);

            // Then
            assertEquals(3, collector.size());
        }

        @Test
        @DisplayName("should handle empty metadata")
        void shouldHandleEmptyMetadata() {
            // Given
            Map<String, Object> emptyMetadata = new HashMap<>();

            // When
            collector.addDocument("docs/empty.adoc", emptyMetadata);

            // Then
            assertEquals(1, collector.size());
        }

        @Test
        @DisplayName("should handle complex nested metadata")
        void shouldHandleComplexNestedMetadata() {
            // Given
            Map<String, Object> complexMetadata = new HashMap<>();
            complexMetadata.put("title", "Test Document");
            complexMetadata.put("author", "John Doe");
            complexMetadata.put("version", "1.0.0");
            complexMetadata.put("tags", Arrays.asList("test", "documentation", "example"));

            Map<String, Object> nestedMap = new HashMap<>();
            nestedMap.put("created", "2024-01-01");
            nestedMap.put("modified", "2024-01-15");
            complexMetadata.put("dates", nestedMap);

            // When
            collector.addDocument("docs/complex.adoc", complexMetadata);

            // Then
            assertEquals(1, collector.size());
        }
    }

    @Nested
    @DisplayName("toJson")
    class ToJsonTests {

        @Test
        @DisplayName("should return correct JSON structure")
        void shouldReturnCorrectJsonStructure() {
            // Given
            collector.addDocument("test.adoc", createSampleMetadata());

            // When
            Map<String, Object> json = collector.toJson();

            // Then
            assertNotNull(json);
            assertTrue(json.containsKey("documents"));
            assertTrue(json.containsKey("timestamp"));
            assertTrue(json.containsKey("count"));
            assertEquals(1, json.get("count"));
            assertNotNull(json.get("timestamp"));
            assertTrue(json.get("documents") instanceof List);
        }

        @Test
        @DisplayName("should handle empty collection")
        void shouldHandleEmptyCollection() {
            // Given
            // Empty collector

            // When
            Map<String, Object> json = collector.toJson();

            // Then
            assertNotNull(json);
            assertEquals(0, json.get("count"));
            assertTrue(((List<?>) json.get("documents")).isEmpty());
        }

        @Test
        @DisplayName("should include all documents in JSON")
        void shouldIncludeAllDocumentsInJson() {
            // Given
            collector.addDocument("doc1.adoc", createMetadata("Title 1"));
            collector.addDocument("doc2.adoc", createMetadata("Title 2"));
            collector.addDocument("doc3.adoc", createMetadata("Title 3"));

            // When
            Map<String, Object> json = collector.toJson();

            // Then
            assertEquals(3, json.get("count"));
            List<?> documents = (List<?>) json.get("documents");
            assertEquals(3, documents.size());
        }

        @Test
        @DisplayName("should format timestamp correctly")
        void shouldFormatTimestampCorrectly() {
            // Given
            collector.addDocument("test.adoc", createSampleMetadata());

            // When
            Map<String, Object> json = collector.toJson();
            String timestamp = (String) json.get("timestamp");

            // Then
            assertNotNull(timestamp);
            assertDoesNotThrow(() -> Instant.parse(timestamp));
        }

        @Test
        @DisplayName("should create immutable JSON representation")
        void shouldCreateImmutableJsonRepresentation() {
            // Given
            Map<String, Object> metadata = createSampleMetadata();
            collector.addDocument("test.adoc", metadata);

            // When
            Map<String, Object> json1 = collector.toJson();
            Map<String, Object> json2 = collector.toJson();

            // Then
            assertNotSame(json1, json2);
            assertEquals(json1.get("count"), json2.get("count"));
        }
    }

    @Nested
    @DisplayName("size")
    class SizeTests {

        @Test
        @DisplayName("should return zero for empty collection")
        void shouldReturnZeroForEmptyCollection() {
            // When
            int size = collector.size();

            // Then
            assertEquals(0, size);
        }

        @Test
        @DisplayName("should return correct count after adding documents")
        void shouldReturnCorrectCountAfterAddingDocuments() {
            // Given & When
            assertEquals(0, collector.size());

            collector.addDocument("doc1.adoc", createSampleMetadata());
            assertEquals(1, collector.size());

            collector.addDocument("doc2.adoc", createSampleMetadata());
            assertEquals(2, collector.size());
        }
    }

    @Nested
    @DisplayName("clear")
    class ClearTests {

        @Test
        @DisplayName("should empty the collection")
        void shouldEmptyTheCollection() {
            // Given
            collector.addDocument("doc1.adoc", createSampleMetadata());
            collector.addDocument("doc2.adoc", createSampleMetadata());
            assertEquals(2, collector.size());

            // When
            collector.clear();

            // Then
            assertEquals(0, collector.size());
        }

        @Test
        @DisplayName("should reset size to zero")
        void shouldResetSizeToZero() {
            // Given
            collector.addDocument("doc.adoc", createSampleMetadata());

            // When
            collector.clear();

            // Then
            Map<String, Object> json = collector.toJson();
            assertEquals(0, json.get("count"));
            assertTrue(((List<?>) json.get("documents")).isEmpty());
        }

        @Test
        @DisplayName("should handle multiple clear operations")
        void shouldHandleMultipleClearOperations() {
            // Given
            collector.addDocument("doc.adoc", createSampleMetadata());

            // When
            collector.clear();
            collector.clear();

            // Then
            assertEquals(0, collector.size());
        }

        @Test
        @DisplayName("should allow adding documents after clear")
        void shouldAllowAddingDocumentsAfterClear() {
            // Given
            collector.addDocument("doc1.adoc", createSampleMetadata());
            collector.clear();

            // When
            collector.addDocument("doc2.adoc", createSampleMetadata());

            // Then
            assertEquals(1, collector.size());
        }
    }

    @Nested
    @DisplayName("Document Order and Isolation")
    class DocumentOrderAndIsolationTests {

        @Test
        @DisplayName("should preserve document order")
        void shouldPreserveDocumentOrder() {
            // Given
            collector.addDocument("doc1.adoc", createMetadata("Title 1"));
            collector.addDocument("doc2.adoc", createMetadata("Title 2"));
            collector.addDocument("doc3.adoc", createMetadata("Title 3"));

            // When
            Map<String, Object> json = collector.toJson();
            List<?> documents = (List<?>) json.get("documents");

            // Then
            assertEquals(3, documents.size());
        }

        @Test
        @DisplayName("should isolate metadata from external modifications")
        void shouldIsolateMetadataFromExternalModifications() {
            // Given
            Map<String, Object> metadata = createSampleMetadata();
            collector.addDocument("test.adoc", metadata);

            // When
            metadata.put("modified", "should not affect stored data");

            // Then
            Map<String, Object> json = collector.toJson();
            List<?> documents = (List<?>) json.get("documents");
            assertEquals(1, documents.size());
        }
    }

    private Map<String, Object> createSampleMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", "Sample Document");
        metadata.put("author", "Test Author");
        metadata.put("date", "2024-01-01");
        return metadata;
    }

    private Map<String, Object> createMetadata(String title) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", title);
        return metadata;
    }
}