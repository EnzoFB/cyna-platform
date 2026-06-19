package com.cyna.modules.notification.application.eventhandler;

import com.cyna.modules.notification.application.NotificationDispatcher;
import com.cyna.modules.user.application.api.event.EmailChangeRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.EmailVerificationRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.LoginOtpRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.PasswordResetRequestedIntegrationEvent;
import com.cyna.modules.user.application.api.event.SuspiciousAuthActivityDetectedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserEmailChangedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserEmailVerifiedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserPasswordChangedIntegrationEvent;
import com.cyna.modules.user.application.api.event.UserRegisteredIntegrationEvent;
import org.springframework.stereotype.Component;

/**
 * Turns user-module integration events into account/security notifications. The
 * events already carry everything the message needs (email, name, lang, token),
 * so no cross-module read is required here — the handler depends only on the
 * published integration contract and the dispatcher.
 */
@Component
public class UserNotificationHandler {

    private final NotificationDispatcher dispatcher;

    public UserNotificationHandler(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void onUserRegistered(UserRegisteredIntegrationEvent event) {
        // Admin-created accounts are ACTIVE immediately, so they still get a
        // welcome on registration. Self-service registrations no longer raise
        // UserRegistered — their welcome is deferred to UserEmailVerified.
        dispatcher.sendWelcomeEmail(event.email(), event.firstName(), event.lang());
    }

    public void onEmailVerificationRequested(EmailVerificationRequestedIntegrationEvent event) {
        dispatcher.sendEmailVerification(event.email(), event.firstName(), event.rawToken(), event.lang());
    }

    public void onUserEmailVerified(UserEmailVerifiedIntegrationEvent event) {
        dispatcher.sendWelcomeEmail(event.email(), event.firstName(), event.lang());
    }

    public void onPasswordChanged(UserPasswordChangedIntegrationEvent event) {
        dispatcher.sendPasswordChangedAlert(event.email(), event.firstName(), event.lang());
    }

    public void onEmailChanged(UserEmailChangedIntegrationEvent event) {
        dispatcher.sendEmailChangedAlert(event.oldEmail(), event.newEmail(), event.firstName(), event.lang());
    }

    public void onPasswordResetRequested(PasswordResetRequestedIntegrationEvent event) {
        dispatcher.sendPasswordResetEmail(event.email(), event.firstName(), event.rawToken(), event.lang());
    }

    public void onSuspiciousActivity(SuspiciousAuthActivityDetectedIntegrationEvent event) {
        dispatcher.sendSuspiciousActivityAlert(event.email(), event.firstName(), event.lang());
    }

    public void onLoginOtpRequested(LoginOtpRequestedIntegrationEvent event) {
        dispatcher.sendLoginOtpEmail(event.email(), event.otpCode(), event.expiresAt(), event.lang());
    }

    public void onEmailChangeRequested(EmailChangeRequestedIntegrationEvent event) {
        dispatcher.sendEmailChangeConfirmation(event.newEmail(), event.firstName(), event.token(), event.lang());
    }
}
