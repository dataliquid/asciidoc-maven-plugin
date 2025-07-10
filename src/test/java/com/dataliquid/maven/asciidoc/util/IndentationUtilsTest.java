package com.dataliquid.maven.asciidoc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IndentationUtils")
class IndentationUtilsTest {
    
    @Nested
    @DisplayName("removeCommonIndentation")
    class RemoveCommonIndentationTests {
        
        @Test
        @DisplayName("should return null when given null")
        void shouldReturnNullWhenGivenNull() {
            // Given
            String input = null;
            
            // When
            String result = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertNull(result);
        }
        
        @Test
        @DisplayName("should return empty string when given empty string")
        void shouldReturnEmptyStringWhenGivenEmptyString() {
            // Given
            String input = "";
            
            // When
            String result = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals("", result);
        }
        
        @Test
        @DisplayName("should remove common space indentation when all lines are indented")
        void shouldRemoveCommonSpaceIndentationWhenAllLinesIndented() throws IOException {
            // Given
            String input = loadTestFile("remove-common-spaces-test/input.yaml");
            String expected = loadTestFile("remove-common-spaces-test/expected.yaml");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should handle tabs as four spaces when calculating indentation")
        void shouldHandleTabsAsFourSpacesWhenCalculatingIndentation() throws IOException {
            // Given
            String input = loadTestFile("handle-tabs-as-spaces-test/input.json");
            String expected = loadTestFile("handle-tabs-as-spaces-test/expected.json");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should preserve empty lines when processing indentation")
        void shouldPreserveEmptyLinesWhenProcessingIndentation() throws IOException {
            // Given
            String input = loadTestFile("preserve-empty-lines-test/input.yaml");
            String expected = loadTestFile("preserve-empty-lines-test/expected.yaml");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should return original when no common indentation exists")
        void shouldReturnOriginalWhenNoCommonIndentationExists() throws IOException {
            // Given
            String input = loadTestFile("no-common-indentation-test/input.json");
            String expected = loadTestFile("no-common-indentation-test/expected.json");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should handle mixed tabs and spaces correctly")
        void shouldHandleMixedTabsAndSpacesCorrectly() throws IOException {
            // Given
            String input = loadTestFile("mixed-tabs-spaces-test/input.yaml");
            String expected = loadTestFile("mixed-tabs-spaces-test/expected.yaml");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should process all lines indented with different levels")
        void shouldProcessAllLinesIndentedWithDifferentLevels() throws IOException {
            // Given
            String input = loadTestFile("all-lines-indented-test/input.json");
            String expected = loadTestFile("all-lines-indented-test/expected.json");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should handle nested structure with complex indentation")
        void shouldHandleNestedStructureWithComplexIndentation() throws IOException {
            // Given
            String input = loadTestFile("nested-structure-test/input.yaml");
            String expected = loadTestFile("nested-structure-test/expected.yaml");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should preserve relative indentation when removing common prefix")
        void shouldPreserveRelativeIndentationWhenRemovingCommonPrefix() throws IOException {
            // Given
            String input = loadTestFile("preserve-relative-indentation-test/input.yaml");
            String expected = loadTestFile("preserve-relative-indentation-test/expected.yaml");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should handle single line content correctly")
        void shouldHandleSingleLineContentCorrectly() throws IOException {
            // Given
            String input = loadTestFile("single-line-test/input.json");
            String expected = loadTestFile("single-line-test/expected.json");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        @Test
        @DisplayName("should preserve trailing newline when present")
        void shouldPreserveTrailingNewlineWhenPresent() throws IOException {
            // Given
            String input = loadTestFile("trailing-newline-test/input.yaml");
            String expected = loadTestFile("trailing-newline-test/expected.yaml");
            
            // When
            String actual = IndentationUtils.removeCommonIndentation(input);
            
            // Then
            assertEquals(expected, actual);
        }
        
        private String loadTestFile(String path) throws IOException {
            String resourcePath = "/indentation/" + path;
            try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    throw new IOException("Test file not found: " + resourcePath);
                }
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}