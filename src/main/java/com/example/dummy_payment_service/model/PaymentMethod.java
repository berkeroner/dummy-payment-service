package com.example.dummy_payment_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PaymentMethod {
    @JsonProperty("credit_card") CREDIT_CARD,
    @JsonProperty("bank_transfer") BANK_TRANSFER,
    @JsonProperty("cash_on_delivery") CASH_ON_DELIVERY
}
