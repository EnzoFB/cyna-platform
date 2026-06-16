package com.cyna.modules.order.application.query.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderQueryPort {
    Page<AdminOrderProjection> findAllForAdmin(String status, Pageable pageable);
}
