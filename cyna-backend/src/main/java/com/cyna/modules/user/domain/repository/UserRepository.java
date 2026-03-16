package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserId;

import java.util.Optional;

public interface UserRepository {

    void save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
