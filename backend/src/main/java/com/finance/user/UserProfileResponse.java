package com.finance.user;

import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String fullName, String defaultCurrency, String timezone) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getDefaultCurrency(), user.getTimezone());
    }
}
