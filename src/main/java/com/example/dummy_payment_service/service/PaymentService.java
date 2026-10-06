package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentAcceptedResponse;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.dto.RefundRequest;
import com.example.dummy_payment_service.dto.RefundResponse;
import com.example.dummy_payment_service.dto.PaymentStatusResponse;
import com.example.dummy_payment_service.model.PaymentStatus;
import com.example.dummy_payment_service.payment.PaymentStrategyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PaymentService {

    private final PaymentProcessor paymentProcessor;
    private final PaymentStrategyFactory strategyFactory;
    private final ConcurrentMap<UUID, AcceptedPayment> acceptedPayments =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RefundResponse> refunds = new ConcurrentHashMap<>();

    public PaymentService(PaymentProcessor paymentProcessor, PaymentStrategyFactory strategyFactory) {
        this.paymentProcessor = paymentProcessor;
        this.strategyFactory = strategyFactory;
    }

    public PaymentAcceptedResponse accept(UUID idempotencyKey, PaymentRequest request) {
        strategyFactory.get(request.method()).validate(request);
        AcceptedPayment accepted = acceptedPayments.compute(idempotencyKey, (key, existing) -> {
            if (existing != null && !existing.request().equals(request)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency key was already used for a different payment request");
            }
            if (existing != null) {
                return existing;
            }
            UUID paymentId = UUID.randomUUID();
            BigDecimal totalAmount = request.items().stream()
                    .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            paymentProcessor.process(paymentId, key, request, totalAmount);

            return new AcceptedPayment(request, new PaymentAcceptedResponse(
                    paymentId, request.orderId(), key, PaymentStatus.PROCESSING));
        });
        return accepted.response();
    }

    public RefundResponse refund(UUID paymentId, RefundRequest request) {
        AcceptedPayment payment = acceptedPayments.values().stream()
                .filter(candidate -> candidate.response().paymentId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));
        if (payment.request().method() != request.method()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Payment method does not match the original payment");
        }
        return refunds.computeIfAbsent(paymentId, id -> new RefundResponse(
                strategyFactory.get(request.method()).refund(id)));
    }

    public PaymentStatusResponse getStatus(UUID paymentId) {
        AcceptedPayment payment = acceptedPayments.values().stream()
                .filter(candidate -> candidate.response().paymentId().equals(paymentId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));
        return toStatusResponse(payment);
    }

    public PaymentStatusResponse getStatusByIdempotencyKey(UUID idempotencyKey) {
        AcceptedPayment payment = acceptedPayments.get(idempotencyKey);
        if (payment == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Payment not found for idempotency key: " + idempotencyKey);
        }
        return toStatusResponse(payment);
    }

    private PaymentStatusResponse toStatusResponse(AcceptedPayment payment) {
        PaymentAcceptedResponse accepted = payment.response();
        PaymentStatus status = paymentProcessor.getStatus(accepted.paymentId());
        if (status == null) {
            status = PaymentStatus.PROCESSING;
        }
        BigDecimal totalAmount = payment.request().items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PaymentStatusResponse(
                accepted.paymentId(),
                accepted.orderId(),
                accepted.idempotencyKey(),
                status,
                totalAmount,
                payment.request().currency(),
                status == PaymentStatus.PROCESSING ? "Payment is processing" : "Payment " + status.name().toLowerCase());
    }

    private record AcceptedPayment(PaymentRequest request, PaymentAcceptedResponse response) {
    }
}
