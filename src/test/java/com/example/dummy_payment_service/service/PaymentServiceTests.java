package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentAcceptedResponse;
import com.example.dummy_payment_service.dto.PaymentItemRequest;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.dto.RefundRequest;
import com.example.dummy_payment_service.model.Currency;
import com.example.dummy_payment_service.model.PaymentStatus;
import com.example.dummy_payment_service.model.PaymentMethod;
import com.example.dummy_payment_service.payment.BankTransferPaymentStrategy;
import com.example.dummy_payment_service.payment.CashOnDeliveryPaymentStrategy;
import com.example.dummy_payment_service.payment.CreditCardPaymentStrategy;
import com.example.dummy_payment_service.payment.PaymentStrategyFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentServiceTests {

    @Test
    void acceptsPaymentAndCalculatesTotalAmount() {
        RecordingPaymentProcessor processor = new RecordingPaymentProcessor();
        PaymentService service = service(processor);
        PaymentRequest request = new PaymentRequest(
                "order-456",
                PaymentMethod.CREDIT_CARD,
                "token",
                List.of(
                        new PaymentItemRequest("Kulaklik", 2, new BigDecimal("750.00")),
                        new PaymentItemRequest("Kablo", 1, new BigDecimal("100.00"))
                ),
                Currency.TRY
        );

        UUID idempotencyKey = UUID.fromString("22222222-2222-2222-2222-222222222222");
        PaymentAcceptedResponse response = service.accept(idempotencyKey, request);
        PaymentAcceptedResponse replayed = service.accept(idempotencyKey, request);

        assertThat(response.paymentId()).isNotNull();
        assertThat(response.orderId()).isEqualTo("order-456");
        assertThat(response.idempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(response.status()).isEqualTo(PaymentStatus.PROCESSING);
        assertThat(replayed).isEqualTo(response);

        assertThat(processor.paymentId).isEqualTo(response.paymentId());
        assertThat(processor.request).isEqualTo(request);
        assertThat(processor.totalAmount).isEqualByComparingTo("1600.00");
        assertThat(processor.invocationCount).isEqualTo(1);

        processor.providerStatus = PaymentStatus.APPROVED;
        assertThat(service.getStatus(response.paymentId()).status())
                .isEqualTo(PaymentStatus.APPROVED);
        assertThat(service.getStatusByIdempotencyKey(idempotencyKey).paymentId())
                .isEqualTo(response.paymentId());
    }

    @Test
    void rejectsCreditCardPaymentWithoutToken() {
        PaymentService service = service(new RecordingPaymentProcessor());
        PaymentRequest request = new PaymentRequest(
                "order-456",
                PaymentMethod.CREDIT_CARD,
                null,
                List.of(new PaymentItemRequest("Kulaklik", 1, new BigDecimal("750.00"))),
                Currency.TRY);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.accept(UUID.randomUUID(), request))
                .hasMessageContaining("Payment token is required");
    }

    @Test
    void rejectsDifferentRequestForTheSameIdempotencyKey() {
        PaymentService service = service(new RecordingPaymentProcessor());
        UUID idempotencyKey = UUID.randomUUID();
        PaymentRequest first = new PaymentRequest(
                "order-1", PaymentMethod.BANK_TRANSFER, null,
                List.of(new PaymentItemRequest("Urun", 1, new BigDecimal("100.00"))),
                Currency.TRY);
        PaymentRequest different = new PaymentRequest(
                "order-2", PaymentMethod.BANK_TRANSFER, null,
                List.of(new PaymentItemRequest("Urun", 1, new BigDecimal("100.00"))),
                Currency.TRY);
        service.accept(idempotencyKey, first);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.accept(idempotencyKey, different))
                .hasMessageContaining("Idempotency key was already used");
    }

    @Test
    void acceptsBankTransferAndCashOnDeliveryWithoutToken() {
        PaymentService service = service(new RecordingPaymentProcessor());

        for (PaymentMethod method : List.of(
                PaymentMethod.BANK_TRANSFER, PaymentMethod.CASH_ON_DELIVERY)) {
            PaymentRequest request = new PaymentRequest(
                    "order-" + method,
                    method,
                    null,
                    List.of(new PaymentItemRequest("Urun", 1, new BigDecimal("100.00"))),
                    Currency.TRY);

            assertThat(service.accept(UUID.randomUUID(), request).status())
                    .isEqualTo(PaymentStatus.PROCESSING);
        }
    }

    @Test
    void refundsThroughTheOriginalPaymentMethodAndIsIdempotent() {
        PaymentService service = service(new RecordingPaymentProcessor());
        PaymentRequest paymentRequest = new PaymentRequest(
                "order-refund",
                PaymentMethod.BANK_TRANSFER,
                null,
                List.of(new PaymentItemRequest("Urun", 1, new BigDecimal("100.00"))),
                Currency.TRY);
        PaymentAcceptedResponse payment = service.accept(UUID.randomUUID(), paymentRequest);

        var first = service.refund(
                payment.paymentId(), new RefundRequest(PaymentMethod.BANK_TRANSFER));
        var replayed = service.refund(
                payment.paymentId(), new RefundRequest(PaymentMethod.BANK_TRANSFER));

        assertThat(first.refundId()).startsWith("TRANSFER-REF-");
        assertThat(replayed).isEqualTo(first);
    }

    private PaymentService service(PaymentProcessor processor) {
        return new PaymentService(processor, new PaymentStrategyFactory(List.of(
                new CreditCardPaymentStrategy(),
                new BankTransferPaymentStrategy(),
                new CashOnDeliveryPaymentStrategy())));
    }

    private static final class RecordingPaymentProcessor extends PaymentProcessor {

        private UUID paymentId;
        private PaymentRequest request;
        private BigDecimal totalAmount;
        private int invocationCount;
        private PaymentStatus providerStatus = PaymentStatus.PROCESSING;

        private RecordingPaymentProcessor() {
            super(null, 0);
        }

        @Override
        public void process(UUID paymentId, UUID idempotencyKey,
                            PaymentRequest request, BigDecimal totalAmount) {
            this.paymentId = paymentId;
            this.request = request;
            this.totalAmount = totalAmount;
            this.invocationCount++;
        }

        @Override
        public PaymentStatus getStatus(UUID paymentId) {
            return providerStatus;
        }
    }
}
