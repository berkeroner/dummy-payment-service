package com.example.dummy_payment_service.dto;

import com.example.dummy_payment_service.model.Currency;
import com.example.dummy_payment_service.model.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.util.List;

public record PaymentRequest(
        @NotBlank String orderId,
        @NotNull PaymentMethod method,
        String paymentToken,
        @NotEmpty List<@Valid PaymentItemRequest> items,
        @NotNull Currency currency
) {
}
