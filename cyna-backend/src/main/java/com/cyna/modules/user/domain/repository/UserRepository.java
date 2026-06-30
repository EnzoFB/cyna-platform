package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    void save(User user);

    Optional<User> findById(UUID id);

    List<User> findAllByIds(Collection<UUID> ids);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    List<User> findAll(int page, int size);

    long countAll();

    void deleteById(UUID id);
}
