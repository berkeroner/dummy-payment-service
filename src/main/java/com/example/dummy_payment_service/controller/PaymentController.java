package com.example.dummy_payment_service.controller;

import com.example.dummy_payment_service.dto.PaymentAcceptedResponse;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.dto.RefundRequest;
import com.example.dummy_payment_service.dto.RefundResponse;
import com.example.dummy_payment_service.dto.PaymentStatusResponse;
import com.example.dummy_payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentAcceptedResponse> createPayment(
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.accepted().body(paymentService.accept(idempotencyKey, request));
    }

    @PostMapping("/{paymentId}/refunds")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(paymentService.refund(paymentId, request));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getStatus(paymentId));
    }

    @GetMapping("/by-idempotency-key/{idempotencyKey}")
    public ResponseEntity<PaymentStatusResponse> getStatusByIdempotencyKey(
            @PathVariable UUID idempotencyKey) {
        return ResponseEntity.ok(paymentService.getStatusByIdempotencyKey(idempotencyKey));
    }
}
