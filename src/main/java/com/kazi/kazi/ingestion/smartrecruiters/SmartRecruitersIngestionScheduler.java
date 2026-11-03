package com.kazi.kazi.ingestion.smartrecruiters;

import com.kazi.kazi.ingestion.IngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class SmartRecruitersIngestionScheduler {

    private final IngestionService ingestionService;
    private final SmartRecruitersJobSource jobSource;
    private final SmartRecruitersProperties properties;

    SmartRecruitersIngestionScheduler(
        IngestionService ingestionService,
        SmartRecruitersJobSource jobSource,
        SmartRecruitersProperties properties
    ) {
        this.ingestionService = ingestionService;
        this.jobSource = jobSource;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${smartrecruiters.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.companies().forEach(company ->
            ingestionService.ingest(new SmartRecruitersCompanySource(jobSource, company))
        );
    }
}
