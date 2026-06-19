package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.application.api.event.EmailChangeRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.EmailVerificationRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.LoginOtpRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.PasswordResetRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.SuspiciousAuthActivityDetectedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserEmailChangedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserEmailVerifiedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserErasedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserPasswordChangedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserRegisteredIntegrationEvent;
import com.cyna.modules.user.domain.event.EmailChangeRequested;
import com.cyna.modules.user.domain.event.EmailVerificationRequested;
import com.cyna.modules.user.domain.event.LoginOtpRequested;
import com.cyna.modules.user.domain.event.PasswordResetRequested;
import com.cyna.modules.user.domain.event.SuspiciousAuthActivityDetected;
import com.cyna.modules.user.domain.event.UserEmailChanged;
import com.cyna.modules.user.domain.event.UserEmailVerified;
import com.cyna.modules.user.domain.event.UserErased;
import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.modules.user.domain.event.UserRegistered;
import com.cyna.shared.application.IntegrationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates this module's internal {@code domain.event}s into the published
 * {@code application.api.event} integration contract. Runs as a synchronous
 * {@code @EventListener} inside the producing transaction, so a downstream
 * {@code AFTER_COMMIT} subscriber still fires only after commit — identical
 * delivery semantics to subscribing to the domain event directly, but without
 * exposing the domain type across the module boundary.
 *
 * <p>This is the single place the {@code user} module's internal events leak
 * outward; at a service extraction only the {@link IntegrationEventPublisher}
 * implementation changes, not this translator.
 */
@Component
public class UserIntegrationEventTranslator {

    private final IntegrationEventPublisher publisher;

    public UserIntegrationEventTranslator(IntegrationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(UserRegistered e) {
        publisher.publish(new UserRegisteredIntegrationEvent(
                e.userId(), e.email(), e.firstName(), e.role(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(EmailVerificationRequested e) {
        publisher.publish(new EmailVerificationRequestedIntegrationEvent(
                e.userId(), e.email(), e.firstName(), e.rawToken(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(UserEmailVerified e) {
        publisher.publish(new UserEmailVerifiedIntegrationEvent(
                e.userId(), e.email(), e.firstName(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(UserPasswordChanged e) {
        publisher.publish(new UserPasswordChangedIntegrationEvent(
                e.userId(), e.email(), e.firstName(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(UserEmailChanged e) {
        publisher.publish(new UserEmailChangedIntegrationEvent(
                e.userId(), e.oldEmail(), e.newEmail(), e.firstName(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(PasswordResetRequested e) {
        publisher.publish(new PasswordResetRequestedIntegrationEvent(
                e.userId(), e.email(), e.firstName(), e.rawToken(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(SuspiciousAuthActivityDetected e) {
        publisher.publish(new SuspiciousAuthActivityDetectedIntegrationEvent(
                e.userId(), e.email(), e.firstName(), e.lang(), e.reason().name(), e.occurredAt()));
    }

    @EventListener
    public void on(LoginOtpRequested e) {
        publisher.publish(new LoginOtpRequestedIntegrationEvent(
                e.email(), e.otpCode(), e.expiresAt(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(EmailChangeRequested e) {
        publisher.publish(new EmailChangeRequestedIntegrationEvent(
                e.userId(), e.newEmail(), e.firstName(), e.token(), e.lang(), e.occurredAt()));
    }

    @EventListener
    public void on(UserErased e) {
        publisher.publish(new UserErasedIntegrationEvent(e.userId(), e.occurredAt()));
    }
}
