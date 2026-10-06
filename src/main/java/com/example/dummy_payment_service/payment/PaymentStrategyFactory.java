package com.example.dummy_payment_service.payment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.dummy_payment_service.model.PaymentMethod;

@Component
public class PaymentStrategyFactory {

    private final List<PaymentStrategy> strategies;

    public PaymentStrategyFactory(List<PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentStrategy get(PaymentMethod method) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(method))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Unsupported payment method: " + method));
    }
}
