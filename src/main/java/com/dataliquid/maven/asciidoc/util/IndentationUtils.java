package com.dataliquid.maven.asciidoc.util;

/**
 * Utility class for handling indentation in templates and error messages.
 */
public class IndentationUtils {

    /**
     * Removes common leading indentation from all lines while preserving relative
     * indentation. This is similar to stripIndent() but specifically designed for
     * template content.
     *
     * @param  content The content to process
     *
     * @return         Content with common indentation removed
     */
    public static String removeCommonIndentation(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String[] lines = content.split("\n", -1);

        // Find the minimum indentation (excluding empty lines)
        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                int indent = 0;
                for (char c : line.toCharArray()) {
                    if (c == ' ') {
                        indent++;
                    } else if (c == '\t') {
                        // Count tab as 4 spaces
                        indent += 4;
                    } else {
                        break;
                    }
                }
                minIndent = Math.min(minIndent, indent);
            }
        }

        // If no non-empty lines found or no indentation, return original
        if (minIndent == Integer.MAX_VALUE || minIndent == 0) {
            return content;
        }

        // Remove the common indentation from each line
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.trim().isEmpty()) {
                // Remove minIndent worth of spaces/tabs
                int removed = 0;
                int j = 0;
                while (removed < minIndent && j < line.length()) {
                    char c = line.charAt(j);
                    if (c == ' ') {
                        removed++;
                        j++;
                    } else if (c == '\t') {
                        removed += 4;
                        j++;
                    } else {
                        break;
                    }
                }
                result.append(line.substring(j));
            } else {
                // Keep empty lines as-is
                result.append(line);
            }

            // Add newline except for last line
            if (i < lines.length - 1) {
                result.append('\n');
            }
        }

        return result.toString();
    }
}