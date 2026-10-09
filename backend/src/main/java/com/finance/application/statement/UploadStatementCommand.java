package com.finance.application.statement;

import java.util.UUID;

/** password (nullable) only opens an encrypted PDF in memory while it is read; it is never stored, logged or returned. */
public record UploadStatementCommand(UUID accountId, String fileName, String contentType, byte[] content, String password) {
}
