package com.finance.application.statement;

import java.util.UUID;

public record UploadStatementCommand(UUID accountId, String fileName, String contentType, byte[] content) {
}
