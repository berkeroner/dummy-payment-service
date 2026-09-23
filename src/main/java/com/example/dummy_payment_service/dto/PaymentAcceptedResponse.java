package com.example.dummy_payment_service.dto;

import com.example.dummy_payment_service.model.PaymentStatus;

import java.util.UUID;

public record PaymentAcceptedResponse(
        UUID paymentId,
        String orderId,
        PaymentStatus status
) {
}
