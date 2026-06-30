package com.cyna.modules.subscription.interfaces.rest.dto.response;

import java.util.UUID;

/**
 * Tells the storefront whether the authenticated user is still entitled to the
 * free trial on a given product. {@code eligible} is {@code false} once the user
 * has ever subscribed to that product — mirroring the exact rule the payment
 * finalize step applies, so the product-page CTA never promises a trial the
 * checkout will silently refuse.
 */
public record TrialEligibilityResponse(UUID productId, boolean eligible) {}
