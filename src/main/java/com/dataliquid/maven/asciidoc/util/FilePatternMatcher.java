package com.dataliquid.maven.asciidoc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.DirectoryScanner;

public class FilePatternMatcher {

    private final File baseDirectory;
    private final String[] includes;
    private final String[] excludes;

    public FilePatternMatcher(File baseDirectory, String[] includes, String[] excludes) {
        this.baseDirectory = baseDirectory;
        this.includes = includes != null ? includes : new String[] { "**/*.adoc" };
        this.excludes = excludes != null ? excludes : new String[0];
    }

    public List<File> getMatchedFiles() {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(baseDirectory);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.setCaseSensitive(false);
        scanner.scan();

        String[] includedFiles = scanner.getIncludedFiles();
        List<File> files = new ArrayList<>();

        for (String relativePath : includedFiles) {
            files.add(new File(baseDirectory, relativePath));
        }

        return files;
    }

    public boolean hasMatchedFiles() {
        return !getMatchedFiles().isEmpty();
    }
}