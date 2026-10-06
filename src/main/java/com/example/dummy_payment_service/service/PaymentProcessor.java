package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentCallbackRequest;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.PaymentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final CallbackService callbackService;
    private final long processingDelayMs;

    public PaymentProcessor(
            CallbackService callbackService,
            @Value("${payment.processing-delay-ms}") long processingDelayMs) {
        this.callbackService = callbackService;
        this.processingDelayMs = processingDelayMs;
    }

    @Async
    public void process(UUID paymentId, UUID idempotencyKey,
                        PaymentRequest request, BigDecimal totalAmount) {
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Payment processing was interrupted for paymentId={}", paymentId);
            return;
        }

        ProcessingOutcome outcome = randomOutcome();
        if (outcome == ProcessingOutcome.NO_CALLBACK) {
            log.info(
                    "Payment callback intentionally skipped to simulate no response: paymentId={}, orderId={}",
                    paymentId,
                    request.orderId()
            );
            return;
        }

        PaymentStatus status = outcome == ProcessingOutcome.APPROVED
                ? PaymentStatus.APPROVED
                : PaymentStatus.REJECTED;

        PaymentCallbackRequest callback = new PaymentCallbackRequest(
                paymentId,
                request.orderId(),
                idempotencyKey,
                status,
                totalAmount,
                request.currency(),
                status == PaymentStatus.APPROVED ? "Payment approved" : "Payment rejected"
        );

        callbackService.send(callback);
    }

    protected ProcessingOutcome randomOutcome() {
        ProcessingOutcome[] outcomes = ProcessingOutcome.values();
        return outcomes[ThreadLocalRandom.current().nextInt(outcomes.length)];
    }

    protected enum ProcessingOutcome {
        APPROVED,
        REJECTED,
        NO_CALLBACK
    }
}
