package com.example.dummy_payment_service.payment;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.PaymentMethod;

@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.CREDIT_CARD;
    }

    @Override
    public void validate(PaymentRequest request) {
        if (request.paymentToken() == null || request.paymentToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Payment token is required");
        }
    }

    @Override
    public String refund(UUID paymentId) {
        return "CARD-REF-" + UUID.randomUUID();
    }
}
