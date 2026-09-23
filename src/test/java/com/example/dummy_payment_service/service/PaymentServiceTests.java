package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentAcceptedResponse;
import com.example.dummy_payment_service.dto.PaymentItemRequest;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.model.Currency;
import com.example.dummy_payment_service.model.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTests {

    @Test
    void acceptsPaymentAndCalculatesTotalAmount() {
        RecordingPaymentProcessor processor = new RecordingPaymentProcessor();
        PaymentService service = new PaymentService(processor);
        PaymentRequest request = new PaymentRequest(
                "order-456",
                List.of(
                        new PaymentItemRequest("Kulaklik", 2, new BigDecimal("750.00")),
                        new PaymentItemRequest("Kablo", 1, new BigDecimal("100.00"))
                ),
                Currency.TRY
        );

        PaymentAcceptedResponse response = service.accept(request);

        assertThat(response.paymentId()).isNotNull();
        assertThat(response.orderId()).isEqualTo("order-456");
        assertThat(response.status()).isEqualTo(PaymentStatus.PROCESSING);

        assertThat(processor.paymentId).isEqualTo(response.paymentId());
        assertThat(processor.request).isEqualTo(request);
        assertThat(processor.totalAmount).isEqualByComparingTo("1600.00");
    }

    private static final class RecordingPaymentProcessor extends PaymentProcessor {

        private UUID paymentId;
        private PaymentRequest request;
        private BigDecimal totalAmount;

        private RecordingPaymentProcessor() {
            super(null, 0);
        }

        @Override
        public void process(UUID paymentId, PaymentRequest request, BigDecimal totalAmount) {
            this.paymentId = paymentId;
            this.request = request;
            this.totalAmount = totalAmount;
        }
    }
}
