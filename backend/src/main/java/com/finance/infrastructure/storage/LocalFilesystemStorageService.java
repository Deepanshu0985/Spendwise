package com.finance.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalFilesystemStorageService implements StorageService {

    private final Path baseDirectory;

    public LocalFilesystemStorageService(@Value("${storage.local-directory}") String localDirectory) {
        this.baseDirectory = Path.of(localDirectory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create local storage directory: " + baseDirectory, e);
        }
    }

    @Override
    public String store(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write statement file to storage: " + key, e);
        }
        return key;
    }

    @Override
    public byte[] retrieve(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read statement file from storage: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete statement file from storage: " + key, e);
        }
    }

    // Keys are constructed internally from UUIDs (StatementServiceImpl), never taken
    // verbatim from user input - this check is defense-in-depth, not the primary guard.
    private Path resolve(String key) {
        Path target = baseDirectory.resolve(key).normalize();
        if (!target.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return target;
    }
}
