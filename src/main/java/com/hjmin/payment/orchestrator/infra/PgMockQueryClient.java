package com.hjmin.payment.orchestrator.infra;


import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class PgMockQueryClient {

    private final RestClient restClient;

    public PgMockQueryClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public PgTxStatusResponse getTxStatus(UUID txId) {
        return restClient.get()
                .uri("/pg/tx/{txId}", txId)
                .retrieve()
                .body(PgTxStatusResponse.class);
    }

    public record PgTxStatusResponse(
            UUID txId,
            String state,
            String resultCode,
            String message
    ) {}
}
