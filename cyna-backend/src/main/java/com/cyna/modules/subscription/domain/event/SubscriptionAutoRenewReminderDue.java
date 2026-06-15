package com.cyna.modules.subscription.domain.event;

import com.cyna.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Raised by the auto-renew reminder batch for each subscription approaching its
 * renewal date, once the "notice sent" mark has been persisted. The
 * notification module turns it into the reminder email. The recipient details
 * are carried on the event (resolved by the batch from {@code UserQueryApi}) so
 * the notification module needs no extra lookup.
 */
public record SubscriptionAutoRenewReminderDue(
        UUID subscriptionId,
        UUID userId,
        String email,
        String firstName,
        String productName,
        String renewalDate,
        String lang,
        Instant occurredAt
) implements DomainEvent {}
