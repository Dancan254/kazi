package com.kazi.kazi.ingestion.ashby;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class AshbyIngestionScheduler {

    private final IngestionService ingestionService;
    private final AshbyJobSource jobSource;
    private final AshbyProperties properties;

    AshbyIngestionScheduler(
        IngestionService ingestionService,
        AshbyJobSource jobSource,
        AshbyProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${ashby.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.organizations().forEach(organization ->
            ingestionService.ingest(new AshbyOrganizationSource(jobSource, organization))
        );
    }
}
