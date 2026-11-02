package com.kazi.kazi.ingestion.adzuna;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class AdzunaIngestionScheduler {

    private final IngestionService ingestionService;
    private final AdzunaJobSource adzunaJobSource;

    AdzunaIngestionScheduler(IngestionService ingestionService, AdzunaJobSource adzunaJobSource) {
        this.ingestionService = ingestionService;
        this.adzunaJobSource = adzunaJobSource;
    }

    @Scheduled(
        fixedDelayString = "#{@adzunaJobSource.pollInterval().toMillis()}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        ingestionService.ingest(adzunaJobSource);
    }
}
