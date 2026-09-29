package com.finance.infrastructure.storage;

/**
 * Private object storage for uploaded statement PDFs (security.md: "private
 * storage, authorized access only"). LocalFilesystemStorageService is the only
 * implementation for now (dev/test/it profiles, matching environments.md's
 * documented "filesystem storage in place of object storage" for dev) - an
 * S3-compatible (DigitalOcean Spaces, ADR-017) implementation is added once
 * DigitalOcean is actually provisioned, not before.
 */
public interface StorageService {

    /** Stores the content under a key derived from userId/statementId; returns the key to persist on the Statement row. */
    String store(String key, byte[] content);

    byte[] retrieve(String key);

    void delete(String key);
}
