package com.hjmin.payment.orchestrator.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalClientConfig {

    @Bean
    public RestClient externalClient() {
        return RestClient.builder()
                .baseUrl("http://localhost:8081")
                .build();
    }
}