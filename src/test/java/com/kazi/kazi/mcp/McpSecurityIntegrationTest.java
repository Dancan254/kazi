package com.kazi.kazi.mcp;

import com.kazi.kazi.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

@AutoConfigureRestTestClient
@TestPropertySource(properties = "kazi.security.api-key=test-secret")
class McpSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void should_keep_health_and_job_search_public() {
        client.get().uri("/api/health").exchange().expectStatus().isOk();
        client.get().uri("/api/jobs").exchange().expectStatus().isOk();
    }

    @Test
    void should_reject_missing_and_invalid_mcp_credentials() {
        initialize(null).expectStatus().isUnauthorized();
        initialize("wrong-secret").expectStatus().isUnauthorized();
        client.get().uri("/actuator/metrics").exchange().expectStatus().isUnauthorized();
        client.get().uri("/actuator/metrics/http.server.requests").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void should_accept_valid_mcp_credentials() {
        initialize("test-secret").expectStatus().isOk();
        client.get().uri("/actuator/metrics")
            .header("X-API-Key", "test-secret")
            .exchange()
            .expectStatus().isOk();
    }

    private RestTestClient.ResponseSpec initialize(String apiKey) {
        var request = client.post().uri("/mcp")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
            .body("""
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-11-25",
                    "capabilities": {},
                    "clientInfo": {"name": "security-test", "version": "1.0"}
                  }
                }
                """);
        if (apiKey != null) {
            request.header("X-API-Key", apiKey);
        }
        return request.exchange();
    }
}
