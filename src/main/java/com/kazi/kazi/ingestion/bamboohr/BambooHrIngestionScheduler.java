package com.kazi.kazi.ingestion.bamboohr;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class BambooHrIngestionScheduler {

    private final IngestionService ingestionService;
    private final BambooHrJobSource jobSource;
    private final BambooHrProperties properties;

    BambooHrIngestionScheduler(
        IngestionService ingestionService,
        BambooHrJobSource jobSource,
        BambooHrProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${bamboohr.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.companies().forEach(company ->
            ingestionService.ingest(new BambooHrCompanySource(jobSource, company))
        );
    }
}
