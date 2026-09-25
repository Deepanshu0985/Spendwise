package com.finance.infrastructure.web.user;

import com.finance.application.user.UserService;
import com.finance.infrastructure.tenancy.TenantContext;
import com.finance.infrastructure.web.common.ApiResponse;
import com.finance.infrastructure.web.common.CurrentUserGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;
    private final TenantContext tenantContext;

    public UserController(UserService userService, TenantContext tenantContext) {
        this.userService = userService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/api/v1/users/me")
    public ApiResponse<UserProfileResponse> me() {
        return ApiResponse.of(UserProfileResponse.from(userService.getById(CurrentUserGuard.require(tenantContext))));
    }
}
