package com.cyna.shared.infrastructure.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "messages")
@Getter
@Setter
public class BrevoProperties {

    private String apiKey;
    private String url;
    private Sender sender = new Sender();

    @Getter
    @Setter
    public static class Sender {
        private String email;
        private String name;
    }
}
