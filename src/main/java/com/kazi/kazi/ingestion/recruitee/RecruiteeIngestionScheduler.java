package com.kazi.kazi.ingestion.recruitee;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class RecruiteeIngestionScheduler {

    private final IngestionService ingestionService;
    private final RecruiteeJobSource jobSource;
    private final RecruiteeProperties properties;

    RecruiteeIngestionScheduler(
        IngestionService ingestionService,
        RecruiteeJobSource jobSource,
        RecruiteeProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${recruitee.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.companies().forEach(company ->
            ingestionService.ingest(new RecruiteeCompanySource(jobSource, company))
        );
    }
}
