package com.dataliquid.maven.asciidoc.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("IncrementalBuildManager")
class IncrementalBuildManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    private Log mockLog;

    private File workDirectory;
    private IncrementalBuildManager manager;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        MockitoAnnotations.openMocks(this);
        workDirectory = tempDir.toFile();
        manager = new IncrementalBuildManager(workDirectory, mockLog);
    }

    @Nested
    @DisplayName("Constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should create manager with work directory only")
        void shouldCreateManagerWithWorkDirectoryOnly() throws NoSuchAlgorithmException {
            // When
            IncrementalBuildManager defaultManager = new IncrementalBuildManager(workDirectory);

            // Then
            assertNotNull(defaultManager);
        }

        @Test
        @DisplayName("should create manager with custom log")
        void shouldCreateManagerWithCustomLog() {
            // Then
            assertNotNull(manager);
            verifyNoInteractions(mockLog);
        }
    }

    @Nested
    @DisplayName("Hash Cache Loading")
    class HashCacheLoadingTests {

        @Test
        @DisplayName("should load existing hash cache from file")
        void shouldLoadExistingHashCacheFromFile() throws Exception {
            // Given
            Path testFile = tempDir.resolve("test.adoc");
            Files.writeString(testFile, "content");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(outputFile, "output");

            // Calculate and save hash
            IncrementalBuildManager tempManager = new IncrementalBuildManager(workDirectory, mockLog);
            tempManager.updateHash(testFile);
            tempManager.saveHashCache();

            // When
            IncrementalBuildManager newManager = new IncrementalBuildManager(workDirectory, mockLog);

            // Then
            assertFalse(newManager.needsRegeneration(testFile, outputFile));
        }

        @Test
        @DisplayName("should handle corrupt hash cache file")
        void shouldHandleCorruptHashCacheFile() throws Exception {
            // Given
            File hashFile = new File(workDirectory, ".asciidoc.hashes");
            Files.writeString(hashFile.toPath(), "corrupted\ncontent\nthat\nis\nnot\nvalid\nproperties");

            // When
            IncrementalBuildManager newManager = new IncrementalBuildManager(workDirectory, mockLog);

            // Then - verify behavior with corrupt cache
            Path testFile = tempDir.resolve("test.adoc");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(testFile, "content");
            Files.writeString(outputFile, "output");

            assertTrue(newManager.needsRegeneration(testFile, outputFile));
        }

        @Test
        @DisplayName("should start with empty cache when file does not exist")
        void shouldStartWithEmptyCacheWhenFileDoesNotExist() throws NoSuchAlgorithmException {
            // When
            new IncrementalBuildManager(workDirectory, mockLog);

            // Then
            verifyNoInteractions(mockLog);
        }
    }

    @Nested
    @DisplayName("Hash Cache Saving")
    class HashCacheSavingTests {

        @Test
        @DisplayName("should create directory when saving cache")
        void shouldCreateDirectoryWhenSavingCache() throws IOException, NoSuchAlgorithmException {
            // Given
            File newWorkDir = new File(tempDir.toFile(), "newdir");
            assertFalse(newWorkDir.exists());
            IncrementalBuildManager newManager = new IncrementalBuildManager(newWorkDir, mockLog);

            // When
            newManager.saveHashCache();

            // Then
            assertTrue(newWorkDir.exists());
            assertTrue(new File(newWorkDir, ".asciidoc.hashes").exists());
        }

        @Test
        @DisplayName("should overwrite existing cache file")
        void shouldOverwriteExistingCacheFile() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Files.writeString(sourceFile, "content");
            manager.updateHash(sourceFile);
            manager.saveHashCache();

            // When
            Files.writeString(sourceFile, "new content");
            manager.updateHash(sourceFile);
            manager.saveHashCache();

            // Then
            File hashFile = new File(workDirectory, ".asciidoc.hashes");
            Properties props = new Properties();
            props.load(Files.newInputStream(hashFile.toPath()));
            assertNotEquals("abc123", props.getProperty(sourceFile.toString()));
        }
    }

    @Nested
    @DisplayName("needsRegeneration")
    class NeedsRegenerationTests {

        @Test
        @DisplayName("should return true when output file is missing")
        void shouldReturnTrueWhenOutputFileMissing() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(sourceFile, "content");

            // When
            boolean needsRegeneration = manager.needsRegeneration(sourceFile, outputFile);

            // Then
            assertTrue(needsRegeneration);
        }

        @Test
        @DisplayName("should return true when hash has changed")
        void shouldReturnTrueWhenHashChanged() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(sourceFile, "original content");
            Files.writeString(outputFile, "output");
            manager.updateHash(sourceFile);

            // When
            Files.writeString(sourceFile, "modified content");
            boolean needsRegeneration = manager.needsRegeneration(sourceFile, outputFile);

            // Then
            assertTrue(needsRegeneration);
        }

        @Test
        @DisplayName("should return false when hash is unchanged")
        void shouldReturnFalseWhenHashUnchanged() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(sourceFile, "content");
            Files.writeString(outputFile, "output");
            manager.updateHash(sourceFile);

            // When
            boolean needsRegeneration = manager.needsRegeneration(sourceFile, outputFile);

            // Then
            assertFalse(needsRegeneration);
        }

        @Test
        @DisplayName("should return true when source not in cache")
        void shouldReturnTrueWhenSourceNotInCache() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(sourceFile, "content");
            Files.writeString(outputFile, "output");

            // When
            boolean needsRegeneration = manager.needsRegeneration(sourceFile, outputFile);

            // Then
            assertTrue(needsRegeneration);
        }

        @Test
        @DisplayName("should check timestamp when hash matches")
        void shouldCheckTimestampWhenHashMatches() throws Exception {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Path outputFile = tempDir.resolve("test.html");
            Files.writeString(sourceFile, "content");
            Files.writeString(outputFile, "output");
            manager.updateHash(sourceFile);

            // When
            Thread.sleep(100);
            Files
                    .setLastModifiedTime(sourceFile,
                            FileTime.fromMillis(Files.getLastModifiedTime(outputFile).toMillis() + 1000));
            boolean needsRegeneration = manager.needsRegeneration(sourceFile, outputFile);

            // Then
            assertTrue(needsRegeneration);
        }
    }

    @Nested
    @DisplayName("updateHash")
    class UpdateHashTests {

        @Test
        @DisplayName("should add new hash entry")
        void shouldAddNewHashEntry() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Files.writeString(sourceFile, "content");

            // When
            manager.updateHash(sourceFile);
            manager.saveHashCache();

            // Then
            File hashFile = new File(workDirectory, ".asciidoc.hashes");
            Properties props = new Properties();
            props.load(Files.newInputStream(hashFile.toPath()));
            assertNotNull(props.getProperty(sourceFile.toString()));
        }

        @Test
        @DisplayName("should overwrite existing hash entry")
        void shouldOverwriteExistingHashEntry() throws IOException {
            // Given
            Path sourceFile = tempDir.resolve("test.adoc");
            Files.writeString(sourceFile, "content1");
            manager.updateHash(sourceFile);
            String hash1 = getHashFromCache(sourceFile);

            // When
            Files.writeString(sourceFile, "content2");
            manager.updateHash(sourceFile);
            String hash2 = getHashFromCache(sourceFile);

            // Then
            assertNotEquals(hash1, hash2);
        }

        @Test
        @DisplayName("should handle non-existent file gracefully")
        void shouldHandleNonExistentFileGracefully() {
            // Given
            Path nonExistentFile = tempDir.resolve("nonexistent.adoc");

            // When & Then
            assertDoesNotThrow(() -> manager.updateHash(nonExistentFile));
        }
    }

    @Nested
    @DisplayName("removeStaleEntries")
    class RemoveStaleEntriesTests {

        @Test
        @DisplayName("should keep current files in cache")
        void shouldKeepCurrentFilesInCache() throws IOException {
            // Given
            Path file1 = tempDir.resolve("file1.adoc");
            Path file2 = tempDir.resolve("file2.adoc");
            Files.writeString(file1, "content1");
            Files.writeString(file2, "content2");

            manager.updateHash(file1);
            manager.updateHash(file2);

            Map<String, Path> currentFiles = new HashMap<>();
            currentFiles.put(file1.toString(), file1);
            currentFiles.put(file2.toString(), file2);

            // When
            manager.removeStaleEntries(currentFiles);
            manager.saveHashCache();

            // Then
            Properties props = new Properties();
            props.load(Files.newInputStream(new File(workDirectory, ".asciidoc.hashes").toPath()));
            assertEquals(2, props.size());
        }

        @Test
        @DisplayName("should remove stale files from cache")
        void shouldRemoveStaleFilesFromCache() throws IOException {
            // Given
            Path file1 = tempDir.resolve("file1.adoc");
            Path file2 = tempDir.resolve("file2.adoc");
            Path file3 = tempDir.resolve("file3.adoc");
            Files.writeString(file1, "content1");
            Files.writeString(file2, "content2");
            Files.writeString(file3, "content3");

            manager.updateHash(file1);
            manager.updateHash(file2);
            manager.updateHash(file3);

            Map<String, Path> currentFiles = new HashMap<>();
            currentFiles.put(file1.toString(), file1);
            currentFiles.put(file2.toString(), file2);

            // When
            manager.removeStaleEntries(currentFiles);
            manager.saveHashCache();

            // Then
            Properties props = new Properties();
            props.load(Files.newInputStream(new File(workDirectory, ".asciidoc.hashes").toPath()));
            assertEquals(2, props.size());
            assertNotNull(props.getProperty(file1.toString()));
            assertNotNull(props.getProperty(file2.toString()));
            assertNull(props.getProperty(file3.toString()));
        }
    }

    @Nested
    @DisplayName("Hash Calculation")
    class HashCalculationTests {

        @Test
        @DisplayName("should calculate consistent hash for same content")
        void shouldCalculateConsistentHashForSameContent() throws IOException, NoSuchAlgorithmException {
            // Given
            Path file = tempDir.resolve("test.adoc");
            String content = "Test content for hashing";
            Files.writeString(file, content);

            // When
            manager.updateHash(file);
            String hash1 = getHashFromCache(file);

            IncrementalBuildManager manager2 = new IncrementalBuildManager(workDirectory, mockLog);
            manager2.updateHash(file);
            String hash2 = getHashFromCache(file);

            // Then
            assertEquals(hash1, hash2);
        }
    }

    private String getHashFromCache(Path file) throws IOException {
        manager.saveHashCache();
        Properties props = new Properties();
        props.load(Files.newInputStream(new File(workDirectory, ".asciidoc.hashes").toPath()));
        return props.getProperty(file.toString());
    }
}