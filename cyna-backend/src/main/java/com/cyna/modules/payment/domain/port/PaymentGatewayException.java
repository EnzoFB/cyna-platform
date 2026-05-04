package com.cyna.modules.payment.domain.port;

public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
