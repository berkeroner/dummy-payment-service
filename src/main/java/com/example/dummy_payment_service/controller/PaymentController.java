package com.example.dummy_payment_service.controller;

import com.example.dummy_payment_service.dto.PaymentAcceptedResponse;
import com.example.dummy_payment_service.dto.PaymentRequest;
import com.example.dummy_payment_service.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentAcceptedResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.accepted().body(paymentService.accept(request));
    }
}
