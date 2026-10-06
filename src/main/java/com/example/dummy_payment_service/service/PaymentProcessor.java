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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);

    private final CallbackService callbackService;
    private final long processingDelayMs;
    private final ConcurrentMap<UUID, PaymentStatus> paymentStatuses = new ConcurrentHashMap<>();

    public PaymentProcessor(
            CallbackService callbackService,
            @Value("${payment.processing-delay-ms}") long processingDelayMs) {
        this.callbackService = callbackService;
        this.processingDelayMs = processingDelayMs;
    }

    @Async
    public void process(UUID paymentId, UUID idempotencyKey,
                        PaymentRequest request, BigDecimal totalAmount) {
        paymentStatuses.put(paymentId, PaymentStatus.PROCESSING);
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Payment processing was interrupted for paymentId={}", paymentId);
            return;
        }

        ProcessingOutcome outcome = randomOutcome();
        if (outcome == ProcessingOutcome.NO_CALLBACK) {
            // The provider completed the payment, but its callback is deliberately
            // skipped. The merchant can recover the result through status polling.
            paymentStatuses.put(paymentId, PaymentStatus.APPROVED);
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
        paymentStatuses.put(paymentId, status);

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

    public PaymentStatus getStatus(UUID paymentId) {
        return paymentStatuses.get(paymentId);
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
