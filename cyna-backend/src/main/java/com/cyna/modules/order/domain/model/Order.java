package com.cyna.modules.order.domain.model;

import com.cyna.modules.order.domain.event.OrderCancelled;
import com.cyna.modules.order.domain.event.OrderCreated;
import com.cyna.modules.order.domain.event.OrderPaid;
import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order extends AggregateRoot<UUID> {

    private static final BigDecimal VAT_RATE = new BigDecimal("0.20");

    private final UUID userId;
    private final OrderStatus status;
    private final List<OrderLine> lines;
    private final Money subtotal;
    private final Money vatAmount;
    private final Money totalTtc;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Order(UUID id,
                  UUID userId,
                  OrderStatus status,
                  List<OrderLine> lines,
                  Money subtotal,
                  Money vatAmount,
                  Money totalTtc,
                  Instant createdAt,
                  Instant updatedAt) {
        super(id);
        Guard.againstNull(id, "id");
        Guard.againstNull(userId, "userId");
        Guard.againstNull(status, "status");
        Guard.againstNull(lines, "lines");
        Guard.againstNull(subtotal, "subtotal");
        Guard.againstNull(vatAmount, "vatAmount");
        Guard.againstNull(totalTtc, "totalTtc");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line");
        }

        this.userId = userId;
        this.status = status;
        this.lines = List.copyOf(lines);
        this.subtotal = subtotal;
        this.vatAmount = vatAmount;
        this.totalTtc = totalTtc;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(UUID userId, List<OrderLine> lines) {
        Guard.againstNull(userId, "userId");
        Guard.againstNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line");
        }

        OrderTotals totals = calculateTotals(lines);
        Instant now = Instant.now();
        Order order = new Order(
                UUID.randomUUID(),
                userId,
                OrderStatus.PENDING,
                lines,
                totals.subtotal(),
                totals.vatAmount(),
                totals.totalTtc(),
                now,
                now
        );
        order.raise(new OrderCreated(order.getId(), userId, totals.totalTtc().amount(), now));
        return order;
    }

    public static Order reconstitute(UUID id,
                                     UUID userId,
                                     OrderStatus status,
                                     List<OrderLine> lines,
                                     Money subtotal,
                                     Money vatAmount,
                                     Money totalTtc,
                                     Instant createdAt,
                                     Instant updatedAt) {
        return new Order(id, userId, status, lines, subtotal, vatAmount, totalTtc, createdAt, updatedAt);
    }

    public Result<Order> pay() {
        if (status == OrderStatus.PAID || status == OrderStatus.FULFILLED) {
            return Result.failure("Order is already paid");
        }
        if (status == OrderStatus.CANCELLED) {
            return Result.failure("Cannot pay a cancelled order");
        }

        Instant now = Instant.now();
        Order paid = new Order(
                getId(),
                userId,
                OrderStatus.PAID,
                lines,
                subtotal,
                vatAmount,
                totalTtc,
                createdAt,
                now
        );
        paid.raise(new OrderPaid(getId(), userId, totalTtc.amount(), now));
        return Result.success(paid);
    }

    public Result<Order> cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            return Result.failure("Order is already cancelled");
        }
        if (status == OrderStatus.FULFILLED) {
            return Result.failure("Cannot cancel a fulfilled order");
        }
        if (status == OrderStatus.PAID) {
            return Result.failure("Cannot cancel a paid order");
        }

        Instant now = Instant.now();
        Order cancelled = new Order(
                getId(),
                userId,
                OrderStatus.CANCELLED,
                lines,
                subtotal,
                vatAmount,
                totalTtc,
                createdAt,
                now
        );
        cancelled.raise(new OrderCancelled(getId(), userId, reason, now));
        return Result.success(cancelled);
    }

    private static OrderTotals calculateTotals(List<OrderLine> lines) {
        Money subtotal = Money.ZERO_EUR;
        String currency = null;

        for (OrderLine line : lines) {
            Money lineTotal = line.getUnitPrice().multiply(line.getQuantity());
            if (currency == null) {
                currency = lineTotal.currency();
                subtotal = lineTotal;
            } else {
                subtotal = subtotal.add(lineTotal);
            }
        }

        if (currency == null) {
            currency = "EUR";
        }

        BigDecimal vatValue = subtotal.amount().multiply(VAT_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalValue = subtotal.amount().add(vatValue).setScale(2, RoundingMode.HALF_UP);

        Money vatAmount = Money.of(vatValue, currency);
        Money totalTtc = Money.of(totalValue, currency);

        return new OrderTotals(subtotal, vatAmount, totalTtc);
    }

    public UUID getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderLine> getLines() {
        return lines;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getVatAmount() {
        return vatAmount;
    }

    public Money getTotalTtc() {
        return totalTtc;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
