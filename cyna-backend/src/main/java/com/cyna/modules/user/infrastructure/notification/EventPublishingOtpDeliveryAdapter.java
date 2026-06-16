package com.cyna.modules.user.infrastructure.notification;

import com.cyna.modules.user.application.port.OtpDeliveryPort;
import com.cyna.modules.user.domain.event.LoginOtpRequested;
import com.cyna.shared.application.DomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Production {@link OtpDeliveryPort} implementation. Rather than calling the
 * notification module directly (which would make {@code user} depend on
 * {@code notification} while {@code notification} already reacts to user events
 * — a forbidden module cycle), it publishes a {@link LoginOtpRequested} domain
 * event. The notification module subscribes to it like any other event, keeping
 * the dependency strictly one-way (notification → user).
 */
@Component
@Primary
public class EventPublishingOtpDeliveryAdapter implements OtpDeliveryPort {

    private static final Logger log = LoggerFactory.getLogger(EventPublishingOtpDeliveryAdapter.class);

    private final DomainEventPublisher eventPublisher;

    public EventPublishingOtpDeliveryAdapter(DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void sendLoginOtp(String email, String otpCode, Instant expiresAt, String lang) {
        log.info("Publishing login OTP request for {} (expires at {})", email, expiresAt);
        eventPublisher.publish(new LoginOtpRequested(email, otpCode, expiresAt, lang, Instant.now()));
    }
}
