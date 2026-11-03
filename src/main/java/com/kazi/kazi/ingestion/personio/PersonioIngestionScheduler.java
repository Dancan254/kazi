package com.kazi.kazi.ingestion.personio;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class PersonioIngestionScheduler {

    private final IngestionService ingestionService;
    private final PersonioJobSource jobSource;
    private final PersonioProperties properties;

    PersonioIngestionScheduler(
        IngestionService ingestionService,
        PersonioJobSource jobSource,
        PersonioProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${personio.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.companies().forEach(company ->
            ingestionService.ingest(new PersonioCompanySource(jobSource, company))
        );
    }
}
