package com.cyna.modules.notification.infrastructure.mail;

import com.cyna.modules.notification.application.NotificationDispatcher;
import com.cyna.modules.notification.application.mail.OrderConfirmationMail;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;
import java.util.List;
import java.util.Map;

/**
 * Brevo + Thymeleaf implementation of {@link NotificationDispatcher}. Owns all
 * template rendering, i18n resolution and HTTP delivery. This is the relocated
 * former {@code com.cyna.shared.infrastructure.notification.BrevoMailService} —
 * now part of the notification module instead of the shared kernel.
 */
@Service
public class BrevoNotificationDispatcher implements NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(BrevoNotificationDispatcher.class);
    private final BrevoProperties properties;
    private final SpringTemplateEngine templateEngine;
    private final MessageSource messageSource;

    public BrevoNotificationDispatcher(
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
    public void sendOrderConfirmation(OrderConfirmationMail data) {
        Locale locale = Locale.forLanguageTag(data.lang());
        Currency currency = Currency.getInstance(data.currency());

        DateTimeFormatter dateFormatter = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(locale)
                .withZone(ZoneId.of("Europe/Paris"));

        List<Map<String, Object>> lines = new ArrayList<>(data.lines().size());
        for (OrderConfirmationMail.Line line : data.lines()) {
            lines.add(Map.of(
                    "productName", line.productName(),
                    "billingCycle", localizeBillingCycle(line.billingCycle(), locale),
                    "quantity", line.quantity(),
                    "unitPrice", formatMoney(line.unitPrice(), currency, locale),
                    "lineTotal", formatMoney(line.lineTotal(), currency, locale)
            ));
        }

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", data.firstName());
        context.setVariable("orderReference", data.orderReference());
        context.setVariable("placedAt", dateFormatter.format(data.placedAt()));
        context.setVariable("lines", lines);
        // Only the HT subtotal is local data. The authoritative TTC/VAT live on
        // the Stripe invoice (which the customer receives directly from Stripe).
        context.setVariable("subtotalHt", formatMoney(data.subtotalHt(), currency, locale));
        context.setVariable("accountUrl", properties.getUrl() + "/account/orders");

        String html = templateEngine.process("email/order-confirmation", context);

        String subject = messageSource.getMessage(
                "email.orderConfirmation.subject",
                new Object[] { data.orderReference() },
                "Your CYNA order is confirmed",
                locale
        );

        sendMail(data.email(), subject, html);
    }

    private String formatMoney(BigDecimal amount, Currency currency, Locale locale) {
        NumberFormat format = NumberFormat.getCurrencyInstance(locale);
        format.setCurrency(currency);
        return format.format(amount);
    }

    private String localizeBillingCycle(String billingCycle, Locale locale) {
        String key = "email.orderConfirmation.billingCycle." + billingCycle.toLowerCase(Locale.ROOT);
        return messageSource.getMessage(key, null, billingCycle, locale);
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
    public void sendPasswordResetEmail(String email, String firstName, String rawToken, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        String resetUrl = properties.getUrl() + "/reset-password?token=" + rawToken;

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("resetUrl", resetUrl);

        String html = templateEngine.process("email/password-reset", context);

        String subject = messageSource.getMessage(
                "email.passwordReset.subject",
                null,
                "Reset your CYNA password",
                locale
        );

        sendMail(email, subject, html);
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

    @Override
    public void sendSubscriptionPaymentFailed(String email,
                                              String firstName,
                                              String productName,
                                              String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("productName", productName);
        context.setVariable("accountUrl", properties.getUrl() + "/account");

        String html = templateEngine.process("email/subscription-payment-failed", context);

        String subject = messageSource.getMessage(
                "email.subscriptionPaymentFailed.subject",
                null,
                "Action required: subscription payment failed",
                locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendSubscriptionCancellationConfirmation(String email,
                                                         String firstName,
                                                         String productName,
                                                         String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("firstName", firstName);
        context.setVariable("productName", productName);
        context.setVariable("accountUrl", properties.getUrl() + "/account");

        String html = templateEngine.process("email/subscription-cancelled", context);

        String subject = messageSource.getMessage(
                "email.subscriptionCancelled.subject",
                null,
                "Your CYNA subscription has been cancelled",
                locale
        );

        sendMail(email, subject, html);
    }

    @Override
    public void sendContactEmail(String fromEmail, String name, String subject, String message, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("fromEmail", fromEmail);
        context.setVariable("name", name);
        context.setVariable("subject", subject);
        context.setVariable("message", message);

        String html = templateEngine.process("email/contact", context);

        String emailSubject = messageSource.getMessage(
                "email.contact.subject",
                new Object[] { subject },
                "[Contact CYNA] " + subject,
                locale
        );

        String toEmail = properties.getContactToEmail();
        if (toEmail == null || toEmail.isBlank()) {
            log.error("Contact email recipient is not configured. Cannot send contact email from {}", fromEmail);
            return;
        }

        sendMail(toEmail, emailSubject, html, fromEmail);
    }

    @Override
    public void sendContactAcknowledgementEmail(String email, String name, String subject, String lang) {
        Locale locale = Locale.forLanguageTag(lang);

        Context context = new Context();
        context.setLocale(locale);
        context.setVariable("name", name);
        context.setVariable("subject", subject);

        String html = templateEngine.process("email/contact-acknowledgement", context);

        String emailSubject = messageSource.getMessage(
                "email.contactAcknowledgement.subject",
                null,
                "We have received your message",
                locale
        );

        sendMail(email, emailSubject, html);
    }

    private void sendMail(String to, String subject, String html) {
        sendMail(to, subject, html, null);
    }

    private void sendMail(String to, String subject, String html, String replyTo) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("sender", Map.of(
                "name", properties.getSender().getName(),
                "email", properties.getSender().getEmail()
        ));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", subject);
        body.put("htmlContent", html);
        body.put("headers", Map.of("Content-Type", "text/html; charset=UTF-8"));
        if (replyTo != null && !replyTo.isBlank()) {
            body.put("replyTo", Map.of("email", replyTo));
        }
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
