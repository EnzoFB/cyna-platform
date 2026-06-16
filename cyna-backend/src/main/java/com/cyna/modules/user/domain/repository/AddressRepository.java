package com.cyna.modules.user.domain.repository;

import com.cyna.modules.user.domain.model.Address;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository {
    void save(Address address);
    Optional<Address> findById(UUID id);
    List<Address> findAllByUserId(UUID userId);
    void deleteById(UUID id);
    void clearDefaultForUser(UUID userId);
}
