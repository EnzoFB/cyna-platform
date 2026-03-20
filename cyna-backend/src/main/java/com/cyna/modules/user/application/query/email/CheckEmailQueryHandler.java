package com.cyna.modules.user.application.query.email;

import com.cyna.modules.user.domain.model.Email;
import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class CheckEmailQueryHandler implements QueryHandler<CheckEmailQuery, Boolean> {

    private final UserRepository userRepository;

    public CheckEmailQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Boolean handle(CheckEmailQuery query) {
        Email email = new Email(query.email());
        return userRepository.existsByEmail(email);
    }
}
