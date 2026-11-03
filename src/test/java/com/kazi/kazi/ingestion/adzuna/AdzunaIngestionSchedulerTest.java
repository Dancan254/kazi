package com.kazi.kazi.ingestion.adzuna;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kazi.kazi.ingestion.IngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdzunaIngestionSchedulerTest {

    @Mock private IngestionService ingestionService;
    @Mock private AdzunaJobSource jobSource;

    @Test
    void should_skip_ingestion_without_credentials() {
        var scheduler = new AdzunaIngestionScheduler(
            ingestionService,
            jobSource,
            new AdzunaProperties("http://localhost", "", null, "us", "USD", 50)
        );

        scheduler.ingest();

        verify(ingestionService, never()).ingest(jobSource);
    }

    @Test
    void should_ingest_with_credentials() {
        var scheduler = new AdzunaIngestionScheduler(
            ingestionService,
            jobSource,
            new AdzunaProperties("http://localhost", "id", "key", "us", "USD", 50)
        );

        scheduler.ingest();

        verify(ingestionService).ingest(jobSource);
    }
}
