package com.example.dummy_payment_service.dto;

import com.example.dummy_payment_service.model.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record RefundRequest(@NotNull PaymentMethod method) {
}
