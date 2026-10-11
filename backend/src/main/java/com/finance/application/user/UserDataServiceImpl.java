package com.finance.application.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.application.auth.GoogleIdentity;
import com.finance.application.auth.GoogleIdentityVerifier;
import com.finance.application.exception.ApiError;
import com.finance.application.exception.DomainValidationException;
import com.finance.application.exception.NotFoundException;
import com.finance.application.exception.UnauthorizedException;
import com.finance.domain.user.User;
import com.finance.domain.user.UserDataRepository;
import com.finance.domain.user.UserRepository;
import com.finance.infrastructure.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserDataServiceImpl implements UserDataService {

    private static final Logger log = LoggerFactory.getLogger(UserDataServiceImpl.class);
    static final String CONFIRMATION_WORD = "DELETE";

    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleIdentityVerifier googleVerifier;
    private final StorageService storageService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public UserDataServiceImpl(
            UserRepository userRepository,
            UserDataRepository userDataRepository,
            PasswordEncoder passwordEncoder,
            GoogleIdentityVerifier googleVerifier,
            StorageService storageService,
            ObjectMapper objectMapper,
            Clock clock) {
        this.userRepository = userRepository;
        this.userDataRepository = userDataRepository;
        this.passwordEncoder = passwordEncoder;
        this.googleVerifier = googleVerifier;
        this.storageService = storageService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAsJson(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
        Map<String, String> tables = userDataRepository.exportTables(userId);

        List<String> signInMethods = new ArrayList<>();
        if (user.hasPassword()) {
            signInMethods.add("password");
        }
        if (user.getGoogleSubject() != null) {
            signInMethods.add("google");
        }
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getFullName());
        profile.put("defaultCurrency", user.getDefaultCurrency());
        profile.put("timezone", user.getTimezone());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("signInMethods", signInMethods);

        try {
            StringBuilder json = new StringBuilder(64 * 1024);
            json.append("{\"formatVersion\":1,\"exportedAt\":")
                    .append(objectMapper.writeValueAsString(clock.instant().toString()))
                    .append(",\"profile\":")
                    .append(objectMapper.writeValueAsString(profile));
            // The table JSON comes straight from the database as text; each is already a valid JSON array.
            tables.forEach((name, rows) -> json.append(",\"").append(name).append("\":").append(rows));
            json.append('}');
            return json.toString().getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not build the data export", e);
        }
    }

    @Override
    public void deleteAccount(UUID userId, DeleteAccountCommand command) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));

        if (command.confirmation() == null || !CONFIRMATION_WORD.equals(command.confirmation().strip())) {
            throw invalid("confirmation", "type " + CONFIRMATION_WORD + " to confirm");
        }
        if (user.hasPassword()) {
            if (command.password() == null || !passwordEncoder.matches(command.password(), user.getPasswordHash())) {
                throw invalid("password", "is incorrect");
            }
        } else {
            requireSameGoogleAccount(user, command.googleCredential());
        }

        List<String> storageKeys = userDataRepository.eraseAll(userId);

        // The rows are gone for good; now the files. A file that cannot be removed is logged (by count, never by name or
        // owner) so it can be cleaned up, but never undoes or blocks the deletion the person asked for.
        int failed = 0;
        for (String key : storageKeys) {
            try {
                storageService.delete(key);
            } catch (RuntimeException e) {
                failed++;
            }
        }
        log.info("Account erased: {} stored file(s) removed, {} could not be removed", storageKeys.size() - failed, failed);
    }

    private void requireSameGoogleAccount(User user, String credential) {
        if (user.getGoogleSubject() == null || credential == null || credential.isBlank()) {
            throw invalid("googleCredential", "sign in with Google again to confirm");
        }
        GoogleIdentity identity;
        try {
            identity = googleVerifier.verify(credential);
        } catch (UnauthorizedException e) {
            throw invalid("googleCredential", "could not be verified");
        }
        if (!user.getGoogleSubject().equals(identity.subject())) {
            throw invalid("googleCredential", "belongs to a different Google account");
        }
    }

    private static DomainValidationException invalid(String field, String reason) {
        return new DomainValidationException("One or more fields are invalid.", List.of(new ApiError.Detail(field, reason)));
    }
}
