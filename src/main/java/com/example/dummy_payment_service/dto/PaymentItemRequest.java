package com.example.dummy_payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentItemRequest(
        @NotBlank String productName,
        @NotNull @Positive Integer quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {
}
