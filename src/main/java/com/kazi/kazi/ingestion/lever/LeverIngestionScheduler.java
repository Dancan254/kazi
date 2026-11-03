package com.kazi.kazi.ingestion.lever;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class LeverIngestionScheduler {

    private final IngestionService ingestionService;
    private final LeverJobSource jobSource;
    private final LeverProperties properties;

    LeverIngestionScheduler(
        IngestionService ingestionService,
        LeverJobSource jobSource,
        LeverProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${lever.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.sites().forEach(site ->
            ingestionService.ingest(new LeverSiteSource(jobSource, site))
        );
    }
}
