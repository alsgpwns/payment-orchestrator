package com.hjmin.payment.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaymentOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentOrchestratorApplication.class, args);
    }

}
