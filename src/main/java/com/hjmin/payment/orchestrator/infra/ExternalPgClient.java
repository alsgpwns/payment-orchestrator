package com.hjmin.payment.orchestrator.infra;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ExternalPgClient {

    private final RestClient restClient;

    public ExternalPgClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public PgResponse authorize(PgRequest request) {

        return restClient.post()
                .uri("/pg/authorize")
                .body(request)
                .retrieve()
                .body(PgResponse.class);
    }

    public record PgRequest(String merchantId, long amount, String currency) {}
    public record PgResponse(String resultCode, String approvalNo, String message) {}
}
