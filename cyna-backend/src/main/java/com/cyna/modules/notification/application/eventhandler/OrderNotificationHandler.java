package com.cyna.modules.notification.application.eventhandler;

import com.cyna.modules.notification.application.NotificationDispatcher;
import com.cyna.modules.notification.application.mail.OrderConfirmationMail;
import com.cyna.modules.order.application.api.OrderQueryApi;
import com.cyna.modules.order.application.api.OrderQueryApi.OrderConfirmationView;
import com.cyna.modules.order.domain.event.OrderPaid;
import com.cyna.modules.user.application.api.UserNotificationView;
import com.cyna.modules.user.application.api.UserQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Builds the order-confirmation email after an {@link OrderPaid} event. The
 * order amounts/lines come from {@code OrderQueryApi}, the recipient identity
 * from {@code UserQueryApi} — both <b>published</b> cross-module seams, never the
 * order/user internal repositories. That is the whole point of housing this in
 * the notification module: it reads other modules only through their contracts,
 * so it would survive an extraction into a separate service.
 */
@Component
public class OrderNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationHandler.class);

    private final OrderQueryApi orderQueryApi;
    private final UserQueryApi userQueryApi;
    private final NotificationDispatcher dispatcher;

    public OrderNotificationHandler(OrderQueryApi orderQueryApi,
                                    UserQueryApi userQueryApi,
                                    NotificationDispatcher dispatcher) {
        this.orderQueryApi = orderQueryApi;
        this.userQueryApi = userQueryApi;
        this.dispatcher = dispatcher;
    }

    public void onOrderPaid(OrderPaid event) {
        OrderConfirmationView order = orderQueryApi.findOrderForConfirmation(event.orderId()).orElse(null);
        if (order == null) {
            log.error("[order-confirmation-mail] order not found orderId={}", event.orderId());
            return;
        }

        UserNotificationView user = userQueryApi.findUserForNotification(event.userId()).orElse(null);
        if (user == null) {
            log.error("[order-confirmation-mail] user not found userId={} orderId={}",
                    event.userId(), event.orderId());
            return;
        }

        List<OrderConfirmationMail.Line> lines = order.lines().stream()
                .map(line -> new OrderConfirmationMail.Line(
                        line.productName(),
                        line.billingCycle(),
                        line.quantity(),
                        line.unitPrice(),
                        line.unitPrice().multiply(BigDecimal.valueOf(line.quantity()))
                ))
                .toList();

        dispatcher.sendOrderConfirmation(new OrderConfirmationMail(
                user.email(),
                user.firstName(),
                shortReference(event.orderId()),
                event.occurredAt(),
                lines,
                order.subtotalHt(),
                order.currency(),
                user.lang()
        ));
    }

    private static String shortReference(UUID orderId) {
        return "CYNA-" + orderId.toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
