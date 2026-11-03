package com.kazi.kazi.ingestion.workable;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class WorkableIngestionScheduler {

    private final IngestionService ingestionService;
    private final WorkableJobSource jobSource;
    private final WorkableProperties properties;

    WorkableIngestionScheduler(
        IngestionService ingestionService,
        WorkableJobSource jobSource,
        WorkableProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${workable.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.accounts().forEach(account ->
            ingestionService.ingest(new WorkableAccountSource(jobSource, account))
        );
    }
}
