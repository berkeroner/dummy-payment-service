package com.example.dummy_payment_service.dto;

import com.example.dummy_payment_service.model.Currency;
import com.example.dummy_payment_service.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCallbackRequest(
        UUID paymentId,
        String orderId,
        PaymentStatus status,
        BigDecimal totalAmount,
        Currency currency,
        String message
) {
}
