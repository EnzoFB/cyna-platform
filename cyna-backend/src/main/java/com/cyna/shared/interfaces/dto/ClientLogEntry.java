package com.cyna.shared.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Optional;

public record ClientLogEntry(
    @NotNull Instant timestamp,
    @NotBlank @Pattern(regexp = "^(ERROR|WARN|INFO|DEBUG)$", message = "Level must be ERROR, WARN, INFO or DEBUG") String level,
    @NotBlank @Size(max = 2000) String message,
    Optional<String> url,
    Optional<@Size(max = 4000) String> stack,
    Optional<String> userAgent,
    Optional<String> correlationId,
    Optional<String> appVersion
) {
}
