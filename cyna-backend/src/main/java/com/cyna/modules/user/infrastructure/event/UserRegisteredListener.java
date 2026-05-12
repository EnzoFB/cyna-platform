package com.cyna.modules.user.infrastructure.event;

import com.cyna.modules.user.domain.event.UserRegistered;
import com.cyna.shared.application.notification.MailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredListener {

    private final MailService mailService;

    public UserRegisteredListener(MailService mailService) {
        this.mailService = mailService;
    }

    @EventListener
    public void handle(UserRegistered event) {
        mailService.sendWelcomeEmail(
            event.email(),
            event.firstName(),
            event.lang()
        );
    }
}
