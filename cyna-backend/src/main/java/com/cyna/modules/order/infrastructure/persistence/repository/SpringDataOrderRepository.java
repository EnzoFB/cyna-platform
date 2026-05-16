package com.cyna.modules.order.infrastructure.persistence.repository;

import com.cyna.modules.order.application.query.admin.AdminOrderProjection;
import com.cyna.modules.order.application.query.admin.AdminOrderQueryPort;
import com.cyna.modules.order.infrastructure.persistence.entity.OrderJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<OrderJpaEntity, UUID>, AdminOrderQueryPort {
    @EntityGraph(attributePaths = "lines")
    Page<OrderJpaEntity> findByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = "lines")
    List<OrderJpaEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    @Query(
        nativeQuery = true,
        value = """
            SELECT o.id, o.user_id, o.status, o.subtotal_amount, o.vat_amount, o.total_amount,
                   o.currency, o.created_at, o.updated_at,
                   u.email AS customer_email, u.first_name AS customer_first_name, u.last_name AS customer_last_name,
                   (SELECT COUNT(*) FROM order_schema.order_lines ol WHERE ol.order_id = o.id) AS line_count
            FROM order_schema.orders o
            JOIN user_schema.users u ON u.id = o.user_id
            WHERE (CAST(:status AS VARCHAR) IS NULL OR o.status = :status)
            ORDER BY o.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM order_schema.orders o
            WHERE (CAST(:status AS VARCHAR) IS NULL OR o.status = :status)
            """
    )
    Page<AdminOrderProjection> findAllForAdmin(
            @Param("status") String status,
            Pageable pageable);
}
