package com.example.dummy_payment_service.payment;

import java.util.UUID;

import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.PaymentMethod;

public interface PaymentStrategy {

    boolean supports(PaymentMethod method);

    void validate(PaymentRequest request);

    String refund(UUID paymentId);
}
