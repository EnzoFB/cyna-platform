package com.cyna.modules.user.application.api;

import java.util.UUID;

/**
 * Minimal projection used by other modules to address transactional emails to
 * a user. {@code lang} reflects the user's last-known preferred language; when
 * no preference is stored, the implementation falls back to the platform
 * default (currently {@code "fr"}).
 */
public record UserNotificationView(
        UUID id,
        String email,
        String firstName,
        String lang
) {}
