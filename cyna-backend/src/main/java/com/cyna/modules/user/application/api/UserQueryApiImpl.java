package com.cyna.modules.user.application.api;

import com.cyna.modules.user.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
class UserQueryApiImpl implements UserQueryApi {

    private final UserRepository userRepository;

    UserQueryApiImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserPaymentView> findUserForPayment(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> new UserPaymentView(
                        user.getId(),
                        user.getEmail().value(),
                        user.getFirstName(),
                        user.getLastName()
                ));
    }
}
