package com.cyna.modules.payment.domain.port;

public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
