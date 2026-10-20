package com.example.dummy_payment_service.payment;

import java.util.Arrays;
import java.util.Optional;

public enum CreditCardTestToken {
    SUCCESS("tok_success"),
    DECLINED("tok_declined"),
    TIMEOUT("tok_timeout");

    private final String value;

    CreditCardTestToken(String value) {
        this.value = value;
    }

    public static Optional<CreditCardTestToken> from(String value) {
        return Arrays.stream(values())
                .filter(token -> token.value.equals(value))
                .findFirst();
    }

    public String value() {
        return value;
    }
}
