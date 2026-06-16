package com.cyna.shared.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final LoggingProperties loggingProperties;

    public RequestLoggingFilter(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !loggingProperties.requestLoggingEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String uri = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String correlationId = MDC.get("traceId");
            int status = response.getStatus();
            String method = request.getMethod();
            String clientIp = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            log.atInfo()
                .addKeyValue("method", method)
                .addKeyValue("uri", uri)
                .addKeyValue("status", status)
                .addKeyValue("duration_ms", duration)
                .addKeyValue("traceId", correlationId != null ? correlationId : "none")
                .addKeyValue("clientIp", clientIp != null ? clientIp : "unknown")
                .addKeyValue("userAgent", userAgent != null ? userAgent : "unknown")
                .log("Request completed");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-Ip");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
