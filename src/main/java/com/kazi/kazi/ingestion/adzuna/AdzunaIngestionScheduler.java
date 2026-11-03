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
    private final AdzunaProperties properties;

    AdzunaIngestionScheduler(
        IngestionService ingestionService,
        AdzunaJobSource adzunaJobSource,
        AdzunaProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.adzunaJobSource = adzunaJobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "#{@adzunaJobSource.pollInterval().toMillis()}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        if (hasText(properties.appId()) && hasText(properties.appKey())) {
            ingestionService.ingest(adzunaJobSource);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
