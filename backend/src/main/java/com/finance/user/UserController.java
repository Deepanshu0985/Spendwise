package com.finance.user;

import com.finance.common.ApiResponse;
import com.finance.common.TenantContext;
import com.finance.common.exception.UnauthorizedException;
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
        var userId = tenantContext.currentUserId();
        if (userId == null) {
            throw new UnauthorizedException("Not authenticated.");
        }
        return ApiResponse.of(UserProfileResponse.from(userService.getById(userId)));
    }
}
