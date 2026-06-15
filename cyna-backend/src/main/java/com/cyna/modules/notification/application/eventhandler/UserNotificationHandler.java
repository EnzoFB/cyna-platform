package com.cyna.modules.notification.application.eventhandler;

import com.cyna.modules.notification.application.NotificationDispatcher;
import com.cyna.modules.user.domain.event.EmailChangeRequested;
import com.cyna.modules.user.domain.event.LoginOtpRequested;
import com.cyna.modules.user.domain.event.PasswordResetRequested;
import com.cyna.modules.user.domain.event.SuspiciousAuthActivityDetected;
import com.cyna.modules.user.domain.event.UserEmailChanged;
import com.cyna.modules.user.domain.event.UserPasswordChanged;
import com.cyna.modules.user.domain.event.UserRegistered;
import org.springframework.stereotype.Component;

/**
 * Turns user-module domain events into account/security notifications. The
 * user events already carry everything the message needs (email, name, lang,
 * token), so no cross-module read is required here — the handler only depends on
 * the published events and the dispatcher.
 */
@Component
public class UserNotificationHandler {

    private final NotificationDispatcher dispatcher;

    public UserNotificationHandler(NotificationDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void onUserRegistered(UserRegistered event) {
        dispatcher.sendWelcomeEmail(event.email(), event.firstName(), event.lang());
    }

    public void onPasswordChanged(UserPasswordChanged event) {
        dispatcher.sendPasswordChangedAlert(event.email(), event.firstName(), event.lang());
    }

    public void onEmailChanged(UserEmailChanged event) {
        dispatcher.sendEmailChangedAlert(event.oldEmail(), event.newEmail(), event.firstName(), event.lang());
    }

    public void onPasswordResetRequested(PasswordResetRequested event) {
        dispatcher.sendPasswordResetEmail(event.email(), event.firstName(), event.rawToken(), event.lang());
    }

    public void onSuspiciousActivity(SuspiciousAuthActivityDetected event) {
        dispatcher.sendSuspiciousActivityAlert(event.email(), event.firstName(), event.lang());
    }

    public void onLoginOtpRequested(LoginOtpRequested event) {
        dispatcher.sendLoginOtpEmail(event.email(), event.otpCode(), event.expiresAt(), event.lang());
    }

    public void onEmailChangeRequested(EmailChangeRequested event) {
        dispatcher.sendEmailChangeConfirmation(event.newEmail(), event.firstName(), event.token(), event.lang());
    }
}
