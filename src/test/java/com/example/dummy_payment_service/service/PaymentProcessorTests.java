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
    @Test
    void timeoutTokenDoesNotSendCallback() {
        RecordingCallbackService callbackService = new RecordingCallbackService();
        PaymentProcessor processor = new PaymentProcessor(callbackService, 0);

        processor.process(PAYMENT_ID, IDEMPOTENCY_KEY, request("tok_timeout"), new BigDecimal("1500.00"));

        assertThat(callbackService.callback).isNull();
        assertThat(processor.getStatus(PAYMENT_ID)).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void successTokenSendsApprovedCallback() {
        RecordingCallbackService callbackService = new RecordingCallbackService();
        PaymentProcessor processor = new PaymentProcessor(callbackService, 0);

        processor.process(PAYMENT_ID, IDEMPOTENCY_KEY, request("tok_success"), new BigDecimal("1500.00"));

        assertThat(callbackService.callback).isNotNull();
        assertThat(callbackService.callback.status()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(callbackService.callback.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(callbackService.callback.idempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(processor.getStatus(PAYMENT_ID)).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void declinedTokenSendsRejectedCallback() {
        RecordingCallbackService callbackService = new RecordingCallbackService();
        PaymentProcessor processor = new PaymentProcessor(callbackService, 0);

        processor.process(PAYMENT_ID, IDEMPOTENCY_KEY, request("tok_declined"), new BigDecimal("1500.00"));

        assertThat(callbackService.callback).isNotNull();
        assertThat(callbackService.callback.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(processor.getStatus(PAYMENT_ID)).isEqualTo(PaymentStatus.REJECTED);
    }

    private PaymentRequest request(String token) {
        return new PaymentRequest(
                "order-456",
                PaymentMethod.CREDIT_CARD,
                token,
                List.of(new PaymentItemRequest("Kulaklik", 2, new BigDecimal("750.00"))),
                Currency.TRY
        );
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
