package com.dataliquid.maven.asciidoc.model;

import java.nio.file.Path;

public class ValidationError {
    private final Path file;
    private final String type;
    private final String message;

    public ValidationError(Path file, String type, String message) {
        this.file = file;
        this.type = type;
        this.message = message;
    }

    public Path getFile() {
        return file;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}