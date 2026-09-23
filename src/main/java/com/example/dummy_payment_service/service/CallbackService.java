package com.example.dummy_payment_service.service;

import com.example.dummy_payment_service.dto.PaymentCallbackRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CallbackService {

    private static final Logger log = LoggerFactory.getLogger(CallbackService.class);

    private final RestClient restClient;
    private final String callbackUrl;

    public CallbackService(
            @Value("${payment.callback-url}") String callbackUrl) {
        this.restClient = RestClient.create();
        this.callbackUrl = callbackUrl;
    }

    public void send(PaymentCallbackRequest callback) {
        restClient.post()
                .uri(callbackUrl)
                .body(callback)
                .retrieve()
                .toBodilessEntity();

        log.info(
                "Payment callback sent: paymentId={}, orderId={}, status={}",
                callback.paymentId(),
                callback.orderId(),
                callback.status()
        );
    }
}
