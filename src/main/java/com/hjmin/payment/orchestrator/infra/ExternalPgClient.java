package com.hjmin.payment.orchestrator.infra;

import com.hjmin.payment.orchestrator.infra.external.dto.PgCancelRequest;
import com.hjmin.payment.orchestrator.infra.external.dto.PgCancelResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

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

    public PgCancelResponse cancel(PgCancelRequest req) {
        return restClient.post()
                .uri("/pg/cancel")
                .body(req)
                .retrieve()
                .body(PgCancelResponse.class);
    }


    public record PgRequest(UUID txId, String merchantId, long amount, String currency) {}
    public record PgResponse(String resultCode, String approvalNo, String message) {}
}
