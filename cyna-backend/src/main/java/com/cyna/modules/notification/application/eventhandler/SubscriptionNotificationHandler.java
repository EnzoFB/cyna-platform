package com.cyna.modules.notification.application.eventhandler;

import com.cyna.modules.notification.application.NotificationDispatcher;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi;
import com.cyna.modules.subscription.application.api.SubscriptionQueryApi.SubscriptionNotificationView;
import com.cyna.modules.subscription.domain.event.SubscriptionAutoRenewReminderDue;
import com.cyna.modules.subscription.domain.event.SubscriptionCancelled;
import com.cyna.modules.subscription.domain.event.SubscriptionPaymentFailed;
import com.cyna.modules.user.application.api.UserNotificationView;
import com.cyna.modules.user.application.api.UserQueryApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Builds subscription lifecycle notifications. The product label comes from
 * {@code SubscriptionQueryApi} and the recipient from {@code UserQueryApi} —
 * the subscription events themselves only carry ids. Both reads go through the
 * published cross-module seams.
 */
@Component
public class SubscriptionNotificationHandler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionNotificationHandler.class);

    private final SubscriptionQueryApi subscriptionQueryApi;
    private final UserQueryApi userQueryApi;
    private final NotificationDispatcher dispatcher;

    public SubscriptionNotificationHandler(SubscriptionQueryApi subscriptionQueryApi,
                                           UserQueryApi userQueryApi,
                                           NotificationDispatcher dispatcher) {
        this.subscriptionQueryApi = subscriptionQueryApi;
        this.userQueryApi = userQueryApi;
        this.dispatcher = dispatcher;
    }

    public void onCancelled(SubscriptionCancelled event) {
        Recipient r = resolve(event.subscriptionId(), event.userId(), "subscription-cancelled-mail");
        if (r == null) {
            return;
        }
        dispatcher.sendSubscriptionCancellationConfirmation(r.email(), r.firstName(), r.productName(), r.lang());
    }

    public void onPaymentFailed(SubscriptionPaymentFailed event) {
        Recipient r = resolve(event.subscriptionId(), event.userId(), "subscription-pastdue-mail");
        if (r == null) {
            return;
        }
        dispatcher.sendSubscriptionPaymentFailed(r.email(), r.firstName(), r.productName(), r.lang());
    }

    /**
     * The reminder event is self-contained (the batch already resolved the
     * recipient), so this is a straight pass-through — no enrichment read.
     */
    public void onAutoRenewReminderDue(SubscriptionAutoRenewReminderDue event) {
        dispatcher.sendSubscriptionAutoRenewReminder(
                event.email(), event.firstName(), event.productName(), event.renewalDate(), event.lang());
    }

    private Recipient resolve(UUID subscriptionId, UUID userId, String logTag) {
        SubscriptionNotificationView subscription =
                subscriptionQueryApi.findForNotification(subscriptionId).orElse(null);
        if (subscription == null) {
            log.error("[{}] subscription not found id={}", logTag, subscriptionId);
            return null;
        }
        UserNotificationView user = userQueryApi.findUserForNotification(userId).orElse(null);
        if (user == null) {
            log.error("[{}] user not found userId={} subId={}", logTag, userId, subscriptionId);
            return null;
        }
        return new Recipient(user.email(), user.firstName(), subscription.productName(), user.lang());
    }

    private record Recipient(String email, String firstName, String productName, String lang) {}
}
