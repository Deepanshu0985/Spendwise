package com.finance.domain.user;

import java.util.Optional;
import java.util.UUID;

/** Port - implemented by infrastructure.persistence.user.UserRepositoryImpl. No Spring Data / JPA types here. */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
