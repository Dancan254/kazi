package com.kazi.kazi.apikey;

import com.kazi.kazi.BaseIntegrationTest;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureRestTestClient
@TestPropertySource(properties = "kazi.security.api-key=test-secret")
class ApiKeySecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private ApiKeyService service;

    @Autowired
    private ApiKeyRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearKeys() {
        repository.deleteAll();
    }

    @Test
    void should_store_only_hash_and_non_secret_prefix() {
        var issued = service.create("Ada", "ada@example.com", 30, Set.of(ApiKeyService.JOBS_READ));

        var stored = repository.findById(issued.metadata().id()).orElseThrow();
        assertThat(stored.getKeyHash()).hasSize(64).doesNotContain(issued.plaintext());
        assertThat(issued.plaintext()).startsWith("kazi_" + stored.getKeyPrefix() + "_");
        assertThat(stored.getKeyHash()).isNotEqualTo(issued.plaintext());
    }

    @Test
    void should_allow_jobs_but_not_metrics_with_jobs_scope() {
        var issued = service.create("reader", null, null, null);

        initialize(issued.plaintext()).expectStatus().isOk();
        client.get().uri("/actuator/metrics")
            .header("X-API-Key", issued.plaintext())
            .exchange()
            .expectStatus().isForbidden();
        assertThat(repository.findById(issued.metadata().id()).orElseThrow().getLastUsedAt()).isNotNull();
    }

    @Test
    void should_reject_revoked_key() {
        var issued = service.create("revoked", null, null, null);
        service.revoke(issued.metadata().id());

        initialize(issued.plaintext()).expectStatus().isUnauthorized();
    }

    @Test
    void should_reject_expired_key() {
        var issued = service.create("expired", null, null, null);
        jdbcTemplate.update("UPDATE api_key SET expires_at = NOW() - INTERVAL '1 second' WHERE id = ?", issued.metadata().id());

        initialize(issued.plaintext()).expectStatus().isUnauthorized();
    }

    @Test
    void should_revoke_old_key_and_preserve_owner_when_rotating() {
        var original = service.create("rotation-user", "rotate@example.com", 45, Set.of(ApiKeyService.JOBS_READ));

        var replacement = service.rotate(original.metadata().id(), 60);

        initialize(original.plaintext()).expectStatus().isUnauthorized();
        initialize(replacement.plaintext()).expectStatus().isOk();
        assertThat(replacement.metadata().ownerLabel()).isEqualTo(original.metadata().ownerLabel());
        assertThat(replacement.metadata().ownerEmail()).isEqualTo(original.metadata().ownerEmail());
        assertThat(replacement.metadata().scopes()).isEqualTo(original.metadata().scopes());
    }

    @Test
    void should_restrict_management_to_bootstrap_admin() {
        var userKey = service.create("ordinary-user", null, null, null);
        var request = client.post().uri("/api/admin/api-keys")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"ownerLabel\":\"managed-user\"}");

        request.header("X-API-Key", userKey.plaintext()).exchange().expectStatus().isForbidden();
        client.post().uri("/api/admin/api-keys")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"ownerLabel\":\"managed-user\"}")
            .exchange()
            .expectStatus().isUnauthorized();
        client.post().uri("/api/admin/api-keys")
            .header("X-API-Key", "test-secret")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"ownerLabel\":\"managed-user\"}")
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.apiKey").isNotEmpty()
            .jsonPath("$.metadata.ownerLabel").isEqualTo("managed-user")
            .jsonPath("$.metadata.scopes[0]").isEqualTo("jobs:read");
    }

    @Test
    void should_not_expose_secret_material_when_listing_keys() {
        service.create("listed-user", null, null, null);

        client.get().uri("/api/admin/api-keys")
            .header("X-API-Key", "test-secret")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$[0].keyPrefix").isNotEmpty()
            .jsonPath("$[0].keyHash").doesNotExist()
            .jsonPath("$[0].apiKey").doesNotExist();
    }

    @Test
    void should_return_not_found_when_rotating_unknown_key() {
        client.post().uri("/api/admin/api-keys/{id}/rotation", UUID.randomUUID())
            .header("X-API-Key", "test-secret")
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"expiresInDays\":90}")
            .exchange()
            .expectStatus().isNotFound()
            .expectBody()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.title").isEqualTo("API key not found")
            .jsonPath("$.detail").exists();
    }

    private RestTestClient.ResponseSpec initialize(String apiKey) {
        return client.post().uri("/mcp")
            .header("X-API-Key", apiKey)
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
                    "clientInfo": {"name": "api-key-test", "version": "1.0"}
                  }
                }
                """)
            .exchange();
    }
}
