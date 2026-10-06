package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentCallbackRequest;
import com.example.dummy_payment_service.dto.PaymentItemRequest;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.Currency;
import com.example.dummy_payment_service.model.PaymentStatus;
import com.example.dummy_payment_service.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProcessorTests {

    private static final UUID PAYMENT_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID IDEMPOTENCY_KEY =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final PaymentRequest REQUEST = new PaymentRequest(
            "order-456",
            PaymentMethod.CREDIT_CARD,
            "token",
            List.of(new PaymentItemRequest("Kulaklik", 2, new BigDecimal("750.00"))),
            Currency.TRY
    );

    @Test
    void doesNotSendCallbackForNoCallbackOutcome() {
        RecordingCallbackService callbackService = new RecordingCallbackService();
        PaymentProcessor processor = processorFor(
                callbackService,
                PaymentProcessor.ProcessingOutcome.NO_CALLBACK
        );

        processor.process(PAYMENT_ID, IDEMPOTENCY_KEY, REQUEST, new BigDecimal("1500.00"));

        assertThat(callbackService.callback).isNull();
    }

    @Test
    void sendsApprovedCallbackForApprovedOutcome() {
        RecordingCallbackService callbackService = new RecordingCallbackService();
        PaymentProcessor processor = processorFor(
                callbackService,
                PaymentProcessor.ProcessingOutcome.APPROVED
        );

        processor.process(PAYMENT_ID, IDEMPOTENCY_KEY, REQUEST, new BigDecimal("1500.00"));

        assertThat(callbackService.callback).isNotNull();
        assertThat(callbackService.callback.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(callbackService.callback.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(callbackService.callback.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
    }

    @Test
    void sendsRejectedCallbackForRejectedOutcome() {
        RecordingCallbackService callbackService = new RecordingCallbackService();
        PaymentProcessor processor = processorFor(
                callbackService,
                PaymentProcessor.ProcessingOutcome.REJECTED
        );

        processor.process(PAYMENT_ID, IDEMPOTENCY_KEY, REQUEST, new BigDecimal("1500.00"));

        assertThat(callbackService.callback).isNotNull();
        assertThat(callbackService.callback.status()).isEqualTo(PaymentStatus.REJECTED);
    }

    private PaymentProcessor processorFor(
            RecordingCallbackService callbackService,
            PaymentProcessor.ProcessingOutcome outcome) {
        return new PaymentProcessor(callbackService, 0) {
            @Override
            protected ProcessingOutcome randomOutcome() {
                return outcome;
            }
        };
    }

    private static final class RecordingCallbackService extends CallbackService {

        private PaymentCallbackRequest callback;

        private RecordingCallbackService() {
            super("http://localhost/callback");
        }

        @Override
        public void send(PaymentCallbackRequest callback) {
            this.callback = callback;
        }
    }
}
