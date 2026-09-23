package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentAcceptedResponse;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentProcessor paymentProcessor;

    public PaymentService(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }

    public PaymentAcceptedResponse accept(PaymentRequest request) {
        UUID paymentId = UUID.randomUUID();
        BigDecimal totalAmount = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        paymentProcessor.process(paymentId, request, totalAmount);

        return new PaymentAcceptedResponse(
                paymentId,
                request.orderId(),
                PaymentStatus.PROCESSING
        );
    }
}
