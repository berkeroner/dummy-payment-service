package com.example.dummy_payment_service.payment;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.PaymentMethod;

@Component
public class CashOnDeliveryPaymentStrategy implements PaymentStrategy {

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.CASH_ON_DELIVERY;
    }

    @Override
    public void validate(PaymentRequest request) {
        // Cash on delivery does not require a payment token.
    }

    @Override
    public String refund(UUID paymentId) {
        return "COD-REF-" + UUID.randomUUID();
    }
}
