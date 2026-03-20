package com.cyna.modules.user.application.query.list;

import com.cyna.modules.user.domain.repository.UserRepository;
import com.cyna.shared.application.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GetUsersQueryHandler implements QueryHandler<GetUsersQuery, UsersPage> {

    private final UserRepository userRepository;

    public GetUsersQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UsersPage handle(GetUsersQuery query) {
        List<UserListReadModel> items = userRepository.findAll(query.page(), query.size())
                .stream()
                .map(user -> new UserListReadModel(
                        user.getId().value(),
                        user.getEmail().value(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole().name(),
                        user.getStatus().name(),
                        user.getCreatedAt()
                ))
                .toList();

        long total = userRepository.countAll();
        int totalPages = query.size() > 0 ? (int) Math.ceil((double) total / query.size()) : 0;

        return new UsersPage(items, total, totalPages);
    }
}
