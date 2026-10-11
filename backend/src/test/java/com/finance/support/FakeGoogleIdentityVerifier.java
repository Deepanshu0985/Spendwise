package com.finance.support;

import com.finance.application.auth.GoogleIdentity;
import com.finance.application.auth.GoogleIdentityVerifier;
import com.finance.application.exception.UnauthorizedException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Stands in for Google: a test registers a made-up token for an identity, and only registered tokens verify. No network. */
public class FakeGoogleIdentityVerifier implements GoogleIdentityVerifier {

    public volatile boolean configured = true;
    private final Map<String, GoogleIdentity> tokens = new ConcurrentHashMap<>();

    public String tokenFor(String subject, String email, String name) {
        String token = "fake-google-token-" + subject + "-" + email;
        tokens.put(token, new GoogleIdentity(subject, email, true, name));
        return token;
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public String clientId() {
        return configured ? "fake-client-id.apps.googleusercontent.com" : "";
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        GoogleIdentity identity = tokens.get(idToken);
        if (identity == null) {
            throw new UnauthorizedException("Google sign-in could not be verified. Please try again.");
        }
        return identity;
    }
}
