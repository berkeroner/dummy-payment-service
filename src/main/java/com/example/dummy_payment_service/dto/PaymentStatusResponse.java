package com.example.dummy_payment_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.dummy_payment_service.model.Currency;
import com.example.dummy_payment_service.model.PaymentStatus;

public record PaymentStatusResponse(
        UUID paymentId,
        String orderId,
        UUID idempotencyKey,
        PaymentStatus status,
        BigDecimal totalAmount,
        Currency currency,
        String message
) {
}
