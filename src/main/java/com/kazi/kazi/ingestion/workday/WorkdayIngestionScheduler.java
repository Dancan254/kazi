package com.kazi.kazi.ingestion.workday;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class WorkdayIngestionScheduler {

    private final IngestionService ingestionService;
    private final WorkdayJobSource jobSource;
    private final WorkdayProperties properties;

    WorkdayIngestionScheduler(
        IngestionService ingestionService,
        WorkdayJobSource jobSource,
        WorkdayProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${workday.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.sites().forEach(site ->
            ingestionService.ingest(new WorkdaySiteSource(jobSource, site))
        );
    }
}
