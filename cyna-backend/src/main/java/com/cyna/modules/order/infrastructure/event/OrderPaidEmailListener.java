package com.cyna.modules.order.infrastructure.event;

import com.cyna.modules.order.domain.event.OrderPaid;
import com.cyna.modules.order.domain.model.Order;
import com.cyna.modules.order.domain.model.OrderLine;
import com.cyna.modules.order.domain.repository.OrderRepository;
import com.cyna.modules.user.application.api.UserNotificationView;
import com.cyna.modules.user.application.api.UserQueryApi;
import com.cyna.shared.application.TransactionRunner;
import com.cyna.shared.application.notification.MailService;
import com.cyna.shared.application.notification.OrderConfirmationMail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Sends the order confirmation email once an {@link OrderPaid} event is
 * committed. Fires on {@code AFTER_COMMIT} so a rolled-back payment never
 * yields a misleading confirmation. The handler is idempotent at the
 * delivery layer: Brevo dedup is not guaranteed, so callers must avoid
 * republishing {@code OrderPaid} on retries (the paying transition itself
 * is idempotent via {@code Order.pay()} state checks).
 */
@Component
public class OrderPaidEmailListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidEmailListener.class);

    private final OrderRepository orderRepository;
    private final UserQueryApi userQueryApi;
    private final MailService mailService;
    private final TransactionRunner transactionRunner;

    public OrderPaidEmailListener(OrderRepository orderRepository,
                                  UserQueryApi userQueryApi,
                                  MailService mailService,
                                  TransactionRunner transactionRunner) {
        this.orderRepository = orderRepository;
        this.userQueryApi = userQueryApi;
        this.mailService = mailService;
        this.transactionRunner = transactionRunner;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPaid event) {
        try {
            OrderConfirmationMail payload = transactionRunner.runReturning(() -> buildPayload(event));
            if (payload == null) {
                return;
            }
            mailService.sendOrderConfirmation(payload);
        } catch (Exception e) {
            log.error("[order-confirmation-mail] dispatch failed orderId={} userId={}",
                    event.orderId(), event.userId(), e);
        }
    }

    private OrderConfirmationMail buildPayload(OrderPaid event) {
        Order order = orderRepository.findById(event.orderId()).orElse(null);
        if (order == null) {
            log.error("[order-confirmation-mail] order not found orderId={}", event.orderId());
            return null;
        }

        UserNotificationView user = userQueryApi.findUserForNotification(event.userId()).orElse(null);
        if (user == null) {
            log.error("[order-confirmation-mail] user not found userId={} orderId={}",
                    event.userId(), event.orderId());
            return null;
        }

        List<OrderConfirmationMail.Line> lines = order.getLines().stream()
                .map(OrderPaidEmailListener::toMailLine)
                .toList();

        return new OrderConfirmationMail(
                user.email(),
                user.firstName(),
                shortReference(order.getId()),
                event.occurredAt(),
                lines,
                order.getSubtotal().amount(),
                order.getVatAmount().amount(),
                order.getTotalTtc().amount(),
                order.getTotalTtc().currency(),
                user.lang()
        );
    }

    private static OrderConfirmationMail.Line toMailLine(OrderLine line) {
        return new OrderConfirmationMail.Line(
                line.getProductName(),
                line.getBillingCycle().name(),
                line.getQuantity(),
                line.getUnitPrice().amount(),
                line.getUnitPrice().multiply(line.getQuantity()).amount()
        );
    }

    private static String shortReference(UUID orderId) {
        return "CYNA-" + orderId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
