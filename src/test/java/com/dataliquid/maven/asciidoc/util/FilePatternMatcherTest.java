package com.dataliquid.maven.asciidoc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FilePatternMatcher")
class FilePatternMatcherTest {

    @TempDir
    Path tempDir;

    private File baseDirectory;

    @BeforeEach
    void setUp() {
        baseDirectory = tempDir.toFile();
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create matcher with all parameters")
        void shouldCreateMatcherWithAllParameters() {
            // Given
            String[] includes = { "**/*.adoc", "**/*.asc" };
            String[] excludes = { "**/target/**" };

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, includes, excludes);

            // Then
            assertNotNull(matcher);
        }

        @Test
        @DisplayName("should use default includes when null provided")
        void shouldUseDefaultIncludesWhenNullProvided() throws IOException {
            // Given
            createTestFile("test.adoc");

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, null, new String[0]);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(1, files.size());
            assertEquals("test.adoc", files.get(0).getName());
        }

        @Test
        @DisplayName("should handle null excludes")
        void shouldHandleNullExcludes() throws IOException {
            // Given
            createTestFile("test.adoc");

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" }, null);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(1, files.size());
        }
    }

    @Nested
    @DisplayName("getMatchedFiles")
    class GetMatchedFilesTests {

        @Test
        @DisplayName("should match files with single pattern")
        void shouldMatchFilesWithSinglePattern() throws IOException {
            // Given
            createTestFile("doc1.adoc");
            createTestFile("doc2.adoc");
            createTestFile("readme.md");

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" },
                    new String[0]);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(2, files.size());
            assertTrue(files.stream().allMatch(f -> f.getName().endsWith(".adoc")));
        }

        @Test
        @DisplayName("should match files with multiple include patterns")
        void shouldMatchFilesWithMultipleIncludePatterns() throws IOException {
            // Given
            createTestFile("doc.adoc");
            createTestFile("doc.asc");
            createTestFile("readme.md");
            String[] includes = { "**/*.adoc", "**/*.asc" };

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, includes, new String[0]);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(2, files.size());
        }

        @Test
        @DisplayName("should exclude files matching exclude patterns")
        void shouldExcludeFilesMatchingExcludePatterns() throws IOException {
            // Given
            createTestFile("doc.adoc");
            createTestFile("draft.adoc", "drafts");
            createTestFile("excluded.adoc", "target");
            String[] excludes = { "**/drafts/**", "**/target/**" };

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" }, excludes);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(1, files.size());
            assertEquals("doc.adoc", files.get(0).getName());
        }

        @Test
        @DisplayName("should throw exception when directory does not exist")
        void shouldThrowExceptionWhenDirectoryDoesNotExist() {
            // Given
            File nonExistent = new File(tempDir.toFile(), "nonexistent");
            FilePatternMatcher matcher = new FilePatternMatcher(nonExistent, new String[] { "**/*.adoc" },
                    new String[0]);

            // When & Then
            assertThrows(IllegalStateException.class, () -> matcher.getMatchedFiles());
        }

        @Test
        @DisplayName("should match files case insensitively")
        void shouldMatchFilesCaseInsensitively() throws IOException {
            // Given
            createTestFile("Doc.ADOC");
            createTestFile("test.AdOc");

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" },
                    new String[0]);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(2, files.size());
        }

        @Test
        @DisplayName("should return empty list when no files match")
        void shouldReturnEmptyListWhenNoFilesMatch() {
            // Given
            // Empty directory

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" },
                    new String[0]);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertTrue(files.isEmpty());
        }

        @Test
        @DisplayName("should match files in nested directories")
        void shouldMatchFilesInNestedDirectories() throws IOException {
            // Given
            createTestFile("root.adoc");
            createTestFile("sub1.adoc", "subdir1");
            createTestFile("sub2.adoc", "subdir1/subdir2");

            // When
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" },
                    new String[0]);
            List<File> files = matcher.getMatchedFiles();

            // Then
            assertEquals(3, files.size());
        }
    }

    @Nested
    @DisplayName("hasMatchedFiles")
    class HasMatchedFilesTests {

        @Test
        @DisplayName("should return true when files exist")
        void shouldReturnTrueWhenFilesExist() throws IOException {
            // Given
            createTestFile("test.adoc");
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" },
                    new String[0]);

            // When
            boolean hasFiles = matcher.hasMatchedFiles();

            // Then
            assertTrue(hasFiles);
        }

        @Test
        @DisplayName("should return false when no files exist")
        void shouldReturnFalseWhenNoFilesExist() {
            // Given
            FilePatternMatcher matcher = new FilePatternMatcher(baseDirectory, new String[] { "**/*.adoc" },
                    new String[0]);

            // When
            boolean hasFiles = matcher.hasMatchedFiles();

            // Then
            assertFalse(hasFiles);
        }
    }

    private void createTestFile(String filename) throws IOException {
        createTestFile(filename, null);
    }

    private void createTestFile(String filename, String subdirectory) throws IOException {
        Path directory = subdirectory != null ? tempDir.resolve(subdirectory) : tempDir;

        Files.createDirectories(directory);
        Files.writeString(directory.resolve(filename), "Test content");
    }
}