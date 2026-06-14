package com.cyna.modules.notification.infrastructure.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Brevo (ex-Sendinblue) delivery settings, bound from the {@code messages.*}
 * configuration tree. Lives in the notification module's infrastructure layer —
 * the only place that knows the platform sends mail through Brevo.
 */
@Configuration
@ConfigurationProperties(prefix = "messages")
@Getter
@Setter
public class BrevoProperties {

    private String apiKey;
    private String url;
    private Sender sender = new Sender();
    private String contactToEmail;

    @Getter
    @Setter
    public static class Sender {
        private String email;
        private String name;
    }
}
