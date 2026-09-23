package com.example.dummy_payment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class DummyPaymentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DummyPaymentServiceApplication.class, args);
	}

}
