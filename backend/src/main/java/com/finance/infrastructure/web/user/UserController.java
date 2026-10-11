package com.finance.infrastructure.web.user;

import com.finance.application.user.DeleteAccountCommand;
import com.finance.application.user.UserDataService;
import com.finance.application.user.UserService;
import com.finance.infrastructure.security.SessionCookieFactory;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
public class UserController {

    private final UserService userService;
    private final UserDataService userDataService;
    private final TenantContext tenantContext;
    private final SessionCookieFactory cookieFactory;

    public UserController(
            UserService userService, UserDataService userDataService, TenantContext tenantContext, SessionCookieFactory cookieFactory) {
        this.userService = userService;
        this.userDataService = userDataService;
        this.tenantContext = tenantContext;
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/api/v1/users/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.of(UserProfileResponse.from(userService.getById(CurrentUserGuard.require(tenantContext))));
    }

    /** Everything held about the signed-in person, as a downloadable JSON file. */
    @GetMapping("/api/v1/users/me/export")
    public ResponseEntity<byte[]> export() {
        UUID userId = CurrentUserGuard.require(tenantContext);
        byte[] body = userDataService.exportAsJson(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("spendwise-data-" + LocalDate.now() + ".json").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    /** Erases the account and all its data for good, then ends the session. A POST so the CSRF check covers it. */
    @PostMapping("/api/v1/users/me/delete")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request, HttpServletResponse response) {
        UUID userId = CurrentUserGuard.require(tenantContext);
        userDataService.deleteAccount(userId, new DeleteAccountCommand(request.password(), request.googleCredential(), request.confirmation()));
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }
}
