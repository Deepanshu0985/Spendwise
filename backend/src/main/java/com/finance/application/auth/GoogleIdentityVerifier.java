package com.finance.application.auth;

/** Port: turns the credential Google's sign-in button hands the browser into a trusted identity, or refuses it. */
public interface GoogleIdentityVerifier {

    /** False when no Google client id is configured; sign-in with Google is then simply unavailable. */
    boolean isConfigured();

    /** This app's Google client id (public by design; the browser needs it to draw the button). Empty when not configured. */
    String clientId();

    /** @throws com.finance.application.exception.UnauthorizedException when the token is not a valid, current Google token for this app */
    GoogleIdentity verify(String idToken);
}
