package com.cyna.modules.payment.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        @NotBlank String secretKey,
        @NotBlank String publishableKey,
        @NotBlank String webhookSecret,

        /**
         * Master switch for Stripe Tax. When {@code false} (default) the
         * integration behaves exactly as before — no {@code automatic_tax},
         * no {@code tax_behavior}, no product tax code — so local dev, the CI
         * boot smoke test and any Stripe account that has NOT activated Tax
         * (and declared its tax registrations) keep working. Set to
         * {@code true} only once Stripe Tax is enabled in the dashboard with
         * at least one tax registration, otherwise {@code Subscription.create}
         * fails because Stripe cannot determine a rate.
         */
        @DefaultValue("false") boolean taxEnabled,

        /**
         * Stripe product tax code applied to every product we create while
         * {@link #taxEnabled} is true. Defaults to the generic
         * "Software as a service (SaaS)" code; override per business if the
         * catalog needs a more specific category. Authoritative list:
         * https://docs.stripe.com/tax/tax-codes.
         */
        @DefaultValue("txcd_10103000") String taxCode
) {}
