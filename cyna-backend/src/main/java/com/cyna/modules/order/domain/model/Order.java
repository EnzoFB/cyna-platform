package com.cyna.modules.order.domain.model;

import com.cyna.modules.order.domain.event.OrderCancelled;
import com.cyna.modules.order.domain.event.OrderCreated;
import com.cyna.modules.order.domain.event.OrderPaid;
import com.cyna.shared.domain.AggregateRoot;
import com.cyna.shared.domain.Guard;
import com.cyna.shared.domain.Money;
import com.cyna.shared.domain.Result;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate — owns line snapshots, billing address and lifecycle status,
 * NOT tax. VAT and TTC are determined and billed by Stripe (subscription with
 * automatic_tax), so the order only stores the HT subtotal. Downstream
 * displays (account history, admin dashboard, confirmation email) defer to
 * the Stripe invoice for the authoritative TTC and VAT amounts.
 */
public class Order extends AggregateRoot<UUID> {

    private final UUID userId;
    private final OrderStatus status;
    private final List<OrderLine> lines;
    private final Money subtotalHt;
    private final BillingAddress billingAddress;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Order(UUID id,
                  UUID userId,
                  OrderStatus status,
                  List<OrderLine> lines,
                  Money subtotalHt,
                  BillingAddress billingAddress,
                  Instant createdAt,
                  Instant updatedAt) {
        super(id);
        Guard.againstNull(id, "id");
        Guard.againstNull(userId, "userId");
        Guard.againstNull(status, "status");
        Guard.againstNull(lines, "lines");
        Guard.againstNull(subtotalHt, "subtotalHt");
        Guard.againstNull(createdAt, "createdAt");
        Guard.againstNull(updatedAt, "updatedAt");

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one line");
        }

        this.userId = userId;
        this.status = status;
        this.lines = List.copyOf(lines);
        this.subtotalHt = subtotalHt;
        this.billingAddress = billingAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(UUID userId, List<OrderLine> lines, BillingAddress billingAddress) {
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
                totals.subtotalHt(),
                billingAddress,
                now,
                now
        );
        order.raise(new OrderCreated(order.getId(), userId, totals.subtotalHt().amount(), now));
        return order;
    }

    public static Order reconstitute(UUID id,
                                     UUID userId,
                                     OrderStatus status,
                                     List<OrderLine> lines,
                                     Money subtotalHt,
                                     BillingAddress billingAddress,
                                     Instant createdAt,
                                     Instant updatedAt) {
        return new Order(id, userId, status, lines, subtotalHt, billingAddress, createdAt, updatedAt);
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
                subtotalHt,
                billingAddress,
                createdAt,
                now
        );
        paid.raise(new OrderPaid(getId(), userId, subtotalHt.amount(), now));
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
                subtotalHt,
                billingAddress,
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

        return new OrderTotals(subtotal);
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

    public Money getSubtotalHt() {
        return subtotalHt;
    }

    public BillingAddress getBillingAddress() {
        return billingAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
