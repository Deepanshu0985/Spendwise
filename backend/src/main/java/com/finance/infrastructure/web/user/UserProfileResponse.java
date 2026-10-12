package com.finance.infrastructure.web.user;

import com.finance.domain.user.User;

import java.util.UUID;

/** hasPassword / googleLinked tell the settings page how this person proves who they are before deleting their account. */
public record UserProfileResponse(
        UUID id, String email, String fullName, String defaultCurrency, String timezone, boolean hasPassword, boolean googleLinked) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getDefaultCurrency(),
                user.getTimezone(),
                user.hasPassword(),
                user.getGoogleSubject() != null);
    }
}
