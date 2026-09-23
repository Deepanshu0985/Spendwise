package com.finance.auth;

import com.finance.common.ApiResponse;
import com.finance.common.TenantContext;
import com.finance.common.exception.UnauthorizedException;
import com.finance.user.User;
import com.finance.user.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TenantContext tenantContext;
    private final SessionCookieFactory cookieFactory;

    public AuthController(AuthService authService, TenantContext tenantContext, SessionCookieFactory cookieFactory) {
        this.authService = authService;
        this.tenantContext = tenantContext;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password(), request.fullName());
        return ApiResponse.of(UserProfileResponse.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<UserProfileResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        AuthService.LoginResult result =
                authService.login(request.email(), request.password(), httpRequest.getHeader(HttpHeaders.USER_AGENT));
        httpResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieFactory.create(result.rawSessionToken(), result.expiresAt()).toString());
        return ApiResponse.of(UserProfileResponse.from(result.user()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Object sessionId = request.getAttribute(SessionAuthenticationFilter.CURRENT_SESSION_ID_ATTRIBUTE);
        if (sessionId instanceof UUID id) {
            authService.logout(id);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponse>> listSessions() {
        UUID userId = requireCurrentUserId();
        List<SessionResponse> sessions = authService.listActiveSessions(userId).stream()
                .map(SessionResponse::from)
                .toList();
        return ApiResponse.of(sessions);
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revokeAllSessions(HttpServletResponse response) {
        UUID userId = requireCurrentUserId();
        authService.revokeAllSessions(userId);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        // Always 202, regardless of whether the email exists (authentication-api.md).
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.confirmPasswordReset(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private UUID requireCurrentUserId() {
        UUID userId = tenantContext.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Not authenticated.");
        }
        return userId;
    }
}
