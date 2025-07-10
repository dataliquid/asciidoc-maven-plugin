package com.dataliquid.maven.asciidoc.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

public class IncrementalBuildManager {
    
    private static final String HASH_FILE = ".asciidoc.hashes";
    private static final String SHA_256_ALGORITHM = "SHA-256";
    private final File workDirectory;
    private final Properties hashCache;
    private final MessageDigest digest;
    private final Log log;
    
    public IncrementalBuildManager(File workDirectory) throws NoSuchAlgorithmException {
        this(workDirectory, new SystemStreamLog());
    }
    
    public IncrementalBuildManager(File workDirectory, Log log) throws NoSuchAlgorithmException {
        this.workDirectory = workDirectory;
        this.hashCache = new Properties();
        this.digest = MessageDigest.getInstance(SHA_256_ALGORITHM);
        this.log = log;
        loadHashCache();
    }
    
    private void loadHashCache() {
        File hashFile = new File(workDirectory, HASH_FILE);
        if (hashFile.exists()) {
            try {
                hashCache.load(Files.newInputStream(hashFile.toPath()));
            } catch (IOException e) {
                log.debug("Failed to load hash cache, starting with empty cache: " + e.getMessage());
            }
        }
    }
    
    public void saveHashCache() {
        File hashFile = new File(workDirectory, HASH_FILE);
        try {
            if (!workDirectory.exists()) {
                workDirectory.mkdirs();
            }
            hashCache.store(Files.newOutputStream(hashFile.toPath()), 
                          "AsciiDoc file hashes for incremental build");
        } catch (IOException e) {
            log.warn("Failed to save hash cache: " + e.getMessage());
        }
    }
    
    public boolean needsRegeneration(Path sourceFile, Path outputFile) {
        if (!outputFile.toFile().exists()) {
            return true;
        }
        
        String currentHash = calculateFileHash(sourceFile);
        String cachedHash = hashCache.getProperty(sourceFile.toString());
        
        if (currentHash == null || !currentHash.equals(cachedHash)) {
            return true;
        }
        
        // Check if output is older than source (shouldn't happen with hash, but safety check)
        return sourceFile.toFile().lastModified() > outputFile.toFile().lastModified();
    }
    
    public void updateHash(Path sourceFile) {
        String hash = calculateFileHash(sourceFile);
        if (hash != null) {
            hashCache.setProperty(sourceFile.toString(), hash);
        }
    }
    
    private String calculateFileHash(Path file) {
        try {
            byte[] fileContent = Files.readAllBytes(file);
            byte[] hashBytes = digest.digest(fileContent);
            return bytesToHex(hashBytes);
        } catch (IOException e) {
            return null;
        }
    }
    
    private String bytesToHex(byte[] hashBytes) {
        StringBuilder result = new StringBuilder();
        for (byte byteValue : hashBytes) {
            result.append(String.format("%02x", byteValue));
        }
        return result.toString();
    }
    
    public void removeStaleEntries(Map<String, Path> currentFiles) {
        hashCache.entrySet().removeIf(entry -> 
            !currentFiles.containsKey(entry.getKey().toString())
        );
    }
}