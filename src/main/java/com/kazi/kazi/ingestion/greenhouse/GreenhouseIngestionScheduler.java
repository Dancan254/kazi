package com.kazi.kazi.ingestion.greenhouse;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class GreenhouseIngestionScheduler {

    private final IngestionService ingestionService;
    private final GreenhouseJobSource jobSource;
    private final GreenhouseProperties properties;

    GreenhouseIngestionScheduler(
        IngestionService ingestionService,
        GreenhouseJobSource jobSource,
        GreenhouseProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${greenhouse.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.boards().forEach(board ->
            ingestionService.ingest(new GreenhouseBoardSource(jobSource, board))
        );
    }
}
