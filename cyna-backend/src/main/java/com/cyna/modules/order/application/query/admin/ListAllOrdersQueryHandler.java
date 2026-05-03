package com.cyna.modules.order.application.query.admin;

import com.cyna.shared.application.QueryHandler;
import com.cyna.shared.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class ListAllOrdersQueryHandler implements QueryHandler<ListAllOrdersQuery, Page<AdminOrderReadModel>> {

    private final AdminOrderQueryPort adminOrderQueryPort;

    public ListAllOrdersQueryHandler(AdminOrderQueryPort adminOrderQueryPort) {
        this.adminOrderQueryPort = adminOrderQueryPort;
    }

    @Override
    public Page<AdminOrderReadModel> handle(ListAllOrdersQuery query) {
        int safePage = Math.max(0, query.page());
        int safeSize = Math.min(Math.max(1, query.size()), 100);

        var pageable = PageRequest.of(safePage, safeSize);
        org.springframework.data.domain.Page<AdminOrderProjection> result =
                adminOrderQueryPort.findAllForAdmin(query.status(), pageable);

        var items = result.getContent().stream()
                .map(p -> new AdminOrderReadModel(
                        p.getId(),
                        p.getUserId(),
                        p.getCustomerEmail(),
                        p.getCustomerFirstName(),
                        p.getCustomerLastName(),
                        p.getStatus(),
                        p.getSubtotalAmount(),
                        p.getVatAmount(),
                        p.getTotalAmount(),
                        p.getCurrency(),
                        p.getLineCount() != null ? p.getLineCount() : 0L,
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                ))
                .toList();

        return new Page<>(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
