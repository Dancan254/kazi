package com.kazi.kazi.ingestion.adzuna;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.source.JobSourceException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "adzuna.base-url=http://localhost:1",
    "adzuna.app-id=test-id",
    "adzuna.app-key=test-key",
    "adzuna.retry.max-retries=0",
    "adzuna.retry.delay-ms=1",
    "resilience4j.circuitbreaker.instances.adzuna.sliding-window-size=2",
    "resilience4j.circuitbreaker.instances.adzuna.minimum-number-of-calls=2"
})
class AdzunaResilienceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AdzunaJobSource jobSource;

    @Test
    void should_fail_fast_when_adzuna_circuit_is_open() {
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> jobSource.fetchSince(null))
                .isInstanceOf(JobSourceException.class);
        }

        assertThatThrownBy(() -> jobSource.fetchSince(null))
            .isInstanceOf(JobSourceException.class)
            .hasCauseInstanceOf(CallNotPermittedException.class);
    }
}
