package com.finance.infrastructure.idempotency;

import com.finance.infrastructure.web.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyKeyRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyServiceImpl(IdempotencyKeyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public ResponseEntity<?> executeIdempotent(
            UUID userId, String idempotencyKey, String endpoint, HttpStatus successStatus, Supplier<Object> action) {
        boolean hasKey = idempotencyKey != null && !idempotencyKey.isBlank();

        if (hasKey) {
            Optional<IdempotencyKeyRecord> existing = repository.findByUserIdAndKeyAndEndpoint(userId, idempotencyKey, endpoint);
            if (existing.isPresent()) {
                IdempotencyKeyRecord record = existing.get();
                return ResponseEntity.status(record.getResponseStatus())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(record.getResponseBody());
            }
        }

        Object result = action.get();
        ApiResponse<Object> envelope = ApiResponse.of(result);

        if (hasKey) {
            repository.save(new IdempotencyKeyRecord(userId, idempotencyKey, endpoint, successStatus.value(), serialize(envelope)));
        }

        return ResponseEntity.status(successStatus).body(envelope);
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize response for idempotency storage", e);
        }
    }
}
