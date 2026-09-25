package com.finance.application.user;

import com.finance.domain.user.User;

import java.util.UUID;

public interface UserService {

    User getById(UUID id);
}
