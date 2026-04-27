package com.cyna.modules.user.infrastructure.persistence.repository;

import com.cyna.modules.user.domain.model.Address;
import com.cyna.modules.user.domain.repository.AddressRepository;
import com.cyna.modules.user.infrastructure.persistence.entity.AddressJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAddressRepositoryAdapter implements AddressRepository {

    private final SpringDataAddressRepository springRepo;

    public JpaAddressRepositoryAdapter(SpringDataAddressRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(Address address) {
        springRepo.save(toEntity(address));
    }

    @Override
    public Optional<Address> findById(UUID id) {
        return springRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Address> findAllByUserId(UUID userId) {
        return springRepo.findAllByUserIdOrdered(userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springRepo.deleteById(id);
    }

    @Override
    public void clearDefaultForUser(UUID userId) {
        springRepo.clearDefaultForUser(userId);
    }

    private AddressJpaEntity toEntity(Address a) {
        var e = new AddressJpaEntity();
        e.setId(a.getId());
        e.setUserId(a.getUserId());
        e.setLabel(a.getLabel());
        e.setAddress(a.getAddress());
        e.setAddress2(a.getAddress2());
        e.setZipCode(a.getZipCode());
        e.setCity(a.getCity());
        e.setRegion(a.getRegion());
        e.setCountryCode(a.getCountryCode());
        e.setPhone(a.getPhone());
        e.setDefault(a.isDefault());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    private Address toDomain(AddressJpaEntity e) {
        return Address.reconstitute(
                e.getId(), e.getUserId(), e.getLabel(), e.getAddress(), e.getAddress2(),
                e.getZipCode(), e.getCity(), e.getRegion(), e.getCountryCode(), e.getPhone(),
                e.isDefault(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
