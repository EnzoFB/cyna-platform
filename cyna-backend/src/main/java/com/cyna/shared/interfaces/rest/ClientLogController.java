package com.cyna.shared.interfaces.rest;

import com.cyna.shared.interfaces.dto.ClientLogEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/logs/client")
public class ClientLogController {

    private static final Logger log = LoggerFactory.getLogger("frontend");
    private static final int MAX_BATCH = 50;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]");

    @PostMapping
    public ResponseEntity<Void> ingest(
            @RequestBody @Valid @Size(max = MAX_BATCH) List<ClientLogEntry> entries,
            @AuthenticationPrincipal String userId) {
        for (ClientLogEntry e : entries) {
            Level level;
            try {
                level = Level.valueOf(e.level().toUpperCase());
            } catch (IllegalArgumentException ex) {
                level = Level.INFO;
            }
            String safeMessage = sanitize(e.message());
            var builder = log.atLevel(level)
                             .addKeyValue("source", "frontend");
            e.correlationId().ifPresent(v -> builder.addKeyValue("traceId", v));
            if (userId != null) {
                builder.addKeyValue("userId", userId);
            }
            e.url().ifPresent(v -> builder.addKeyValue("url", v));
            e.appVersion().ifPresent(v -> builder.addKeyValue("appVersion", v));
            builder.log(safeMessage);
        }
        return ResponseEntity.ok().build();
    }

    private static String sanitize(String message) {
        if (message == null) {
            return "[null message]";
        }
        String stripped = CONTROL_CHARS.matcher(message).replaceAll("");
        return stripped.length() > MAX_MESSAGE_LENGTH
                ? stripped.substring(0, MAX_MESSAGE_LENGTH) + "…"
                : stripped;
    }
}
