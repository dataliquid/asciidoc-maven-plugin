package com.dataliquid.maven.asciidoc.util;

/**
 * Utility class for handling indentation in templates and error messages.
 */
public class IndentationUtils {

    private static final char SPACE_CHAR = ' ';
    private static final char TAB_CHAR = '\t';
    private static final int TAB_WIDTH = 4;

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
            if (!isBlank(line)) {
                int indent = 0;
                for (char c : line.toCharArray()) {
                    if (c == SPACE_CHAR) {
                        indent++;
                    } else if (c == TAB_CHAR) {
                        // Count tab as specified tab width
                        indent += TAB_WIDTH;
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
            if (!isBlank(line)) {
                // Remove minIndent worth of spaces/tabs
                int removed = 0;
                int j = 0;
                while (removed < minIndent && j < line.length()) {
                    char c = line.charAt(j);
                    if (c == SPACE_CHAR) {
                        removed++;
                        j++;
                    } else if (c == TAB_CHAR) {
                        removed += TAB_WIDTH;
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

    private static boolean isBlank(String str) {
        if (str == null || str.length() == 0) {
            return true;
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) > SPACE_CHAR) {
                return false;
            }
        }
        return true;
    }
}