package com.cyna.modules.order.application.query.admin;

import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.modules.user.application.api.UserSummaryView;
import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ListAllOrdersQueryHandler implements QueryHandler<ListAllOrdersQuery, Page<AdminOrderReadModel>> {

    private final AdminOrderQueryPort adminOrderQueryPort;
    private final UserQueryApi userQueryApi;

    public ListAllOrdersQueryHandler(AdminOrderQueryPort adminOrderQueryPort, UserQueryApi userQueryApi) {
        this.adminOrderQueryPort = adminOrderQueryPort;
        this.userQueryApi = userQueryApi;
    }

    @Override
    public Page<AdminOrderReadModel> handle(ListAllOrdersQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        var pageable = PageRequest.of(safePage, safeSize);
        org.springframework.data.domain.Page<AdminOrderProjection> result =
                adminOrderQueryPort.findAllForAdmin(query.status(), pageable);

        // Enrich with customer identity via the user module's published API
        // (the order module reads order_schema only — never joins user_schema).
        List<UUID> userIds = result.getContent().stream()
                .map(AdminOrderProjection::getUserId)
                .distinct()
                .toList();
        Map<UUID, UserSummaryView> usersById = userQueryApi.findSummariesByIds(userIds).stream()
                .collect(Collectors.toMap(UserSummaryView::id, Function.identity()));

        var items = result.getContent().stream()
                .map(p -> {
                    UserSummaryView user = usersById.get(p.getUserId());
                    return new AdminOrderReadModel(
                            p.getId(),
                            p.getUserId(),
                            user != null ? user.email() : null,
                            user != null ? user.firstName() : null,
                            user != null ? user.lastName() : null,
                            p.getStatus(),
                            p.getSubtotalAmount(),
                            p.getVatAmount(),
                            p.getTotalAmount(),
                            p.getCurrency(),
                            p.getLineCount() != null ? p.getLineCount() : 0L,
                            p.getCreatedAt(),
                            p.getUpdatedAt()
                    );
                })
                .toList();

        return new Page<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
