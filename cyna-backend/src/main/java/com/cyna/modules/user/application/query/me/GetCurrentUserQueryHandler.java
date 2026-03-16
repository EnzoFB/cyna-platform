package com.cyna.modules.user.application.query.me;

import com.cyna.modules.user.domain.model.User;
import com.cyna.modules.user.domain.model.UserId;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class GetCurrentUserQueryHandler implements QueryHandler<GetCurrentUserQuery, UserReadModel> {

    private final UserRepository userRepository;

    public GetCurrentUserQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserReadModel handle(GetCurrentUserQuery query) {
        Optional<User> userOpt = userRepository.findById(UserId.of(query.userId()));

        return userOpt.map(user -> new UserReadModel(
                user.getId().value(),
                user.getEmail().value(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        )).orElse(null);
    }
}
