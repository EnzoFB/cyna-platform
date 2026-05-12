package com.cyna.shared.infrastructure.notification;

import com.cyna.shared.application.notification.MailService;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import java.util.Map;

@Service
public class BrevoMailService implements MailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoMailService.class);
    private final BrevoProperties properties;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;

    public BrevoMailService(
        BrevoProperties properties,
        SpringTemplateEngine templateEngine,
        MessageSource messageSource
    ) {
        this.properties = properties;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
    }

    @Override
    public void sendWelcomeEmail(String email, String firstName, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("appUrl", properties.getUrl());

        String html = templateEngine.process("email/welcome", context);

        String subject = messageSource.getMessage(
            "email.welcome.subject",
            null,
            "DEFAULT SUBJECT",
            locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendOrderConfirmation(String email, String orderId, String lang) {
        sendMail(email,
            "Commande confirmée",
            "<p>Votre commande #" + orderId + " a été confirmée !</p>");
    }

    @Override
    public void sendEmailChangeConfirmation(String email, String firstName, String token, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        String confirmUrl = properties.getUrl() + "/account?confirmEmail=" + token;

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("confirmUrl", confirmUrl);

        String html = templateEngine.process("email/email-change", context);

        String subject = messageSource.getMessage(
            "email.emailChange.subject",
            null,
            "Confirm your new email address",
            locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendLoginOtpEmail(String email, String otpCode, Instant expiresAt, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        String formattedExpiry = DateTimeFormatter
                .ofPattern("HH:mm")
                .withZone(ZoneId.of("Europe/Paris"))
                .format(expiresAt);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("otpCode", otpCode);
        context.setVariable("expiresAt", formattedExpiry);

        String html = templateEngine.process("email/otp", context);

        String subject = messageSource.getMessage(
                "email.otp.subject",
                null,
                "Your CYNA verification code",
                locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendPasswordChangedAlert(String email, String firstName, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("appUrl", properties.getUrl());

        String html = templateEngine.process("email/password-changed", context);

        String subject = messageSource.getMessage(
                "email.passwordChanged.subject",
                null,
                "Your CYNA password was changed",
                locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendEmailChangedAlert(String previousEmail, String newEmail, String firstName, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("newEmail", newEmail);

        String html = templateEngine.process("email/email-changed", context);

        String subject = messageSource.getMessage(
                "email.emailChanged.subject",
                null,
                "Your CYNA email address was changed",
                locale
        );

        sendMail(previousEmail, subject, html);
    }

    @Override
    public void sendSuspiciousActivityAlert(String email, String firstName, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("appUrl", properties.getUrl());

        String html = templateEngine.process("email/suspicious-activity", context);

        String subject = messageSource.getMessage(
                "email.suspiciousActivity.subject",
                null,
                "Suspicious activity detected on your CYNA account",
                locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendSubscriptionAutoRenewReminder(String email,
                                                  String firstName,
                                                  String productName,
                                                  String renewalDate,
                                                  String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("productName", productName);
        context.setVariable("renewalDate", renewalDate);
        context.setVariable("accountUrl", properties.getUrl() + "/account");

        String html = templateEngine.process("email/subscription-auto-renew-reminder", context);

        String subject = messageSource.getMessage(
                "email.subscriptionAutoRenewReminder.subject",
                null,
                "Automatic renewal reminder",
                locale
        );

        sendMail(email, subject, html);
    }

    private void sendMail(String to, String subject, String html) {
        Map<String, Object> body = Map.of(
            "sender", Map.of(
                "name", properties.getSender().getName(),
                "email", properties.getSender().getEmail()
            ),
            "to", List.of(Map.of("email", to)),
            "subject", subject,
            "htmlContent", html,
            "headers", Map.of("Content-Type", "text/html; charset=UTF-8")
        );
        try {
            HttpResponse<JsonNode> response = Unirest.post("https://api.brevo.com/v3/smtp/email")
                .header("api-key", properties.getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .asJson();

            log.info("Brevo response: {} {}", response.getStatus(), response.getBody());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail à {}", to, e);
        }
    }
}
