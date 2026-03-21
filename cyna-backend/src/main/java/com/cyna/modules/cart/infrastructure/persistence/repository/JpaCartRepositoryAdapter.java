package com.cyna.modules.cart.infrastructure.persistence.repository;

import com.cyna.modules.cart.domain.model.Cart;
import com.cyna.modules.cart.domain.model.CartStatus;
import com.cyna.modules.cart.domain.repository.CartRepository;
import com.cyna.modules.cart.infrastructure.persistence.mapper.CartJpaMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaCartRepositoryAdapter implements CartRepository {

    private final SpringDataCartRepository springRepo;
    private final CartJpaMapper mapper;

    public JpaCartRepositoryAdapter(SpringDataCartRepository springRepo, CartJpaMapper mapper) {
        this.springRepo = springRepo;
        this.mapper = mapper;
    }

    @Override
    public void save(Cart cart) {
        springRepo.save(mapper.toJpa(cart));
    }

    @Override
    public Optional<Cart> findById(UUID cartId) {
        return springRepo.findById(cartId).map(mapper::toDomain);
    }

    @Override
    public Optional<Cart> findActiveByUserId(UUID userId) {
        return springRepo.findByUserIdAndStatus(userId, CartStatus.ACTIVE.name()).map(mapper::toDomain);
    }

    @Override
    public Optional<Cart> findActiveByGuestToken(String guestToken) {
        return springRepo.findByGuestTokenAndStatus(guestToken, CartStatus.ACTIVE.name()).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID cartId) {
        springRepo.deleteById(cartId);
    }
}
