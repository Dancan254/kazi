package com.kazi.kazi.health;

import com.kazi.kazi.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;

@AutoConfigureRestTestClient
class HealthApiIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Test
    void should_return_up_when_application_is_healthy() {
        client.get().uri("/api/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.status").isEqualTo("UP");
    }
}
