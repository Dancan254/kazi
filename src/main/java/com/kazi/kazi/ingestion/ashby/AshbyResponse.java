package com.kazi.kazi.ingestion.ashby;

import java.time.Instant;
import java.util.List;

record AshbyResponse(String apiVersion, List<AshbyJob> jobs) {

    record AshbyJob(
        String id,
        String title,
        String location,
        List<SecondaryLocation> secondaryLocations,
        String employmentType,
        String workplaceType,
        String descriptionHtml,
        String descriptionPlain,
        Instant publishedAt,
        String jobUrl,
        String applyUrl,
        Compensation compensation
    ) {
    }

    record SecondaryLocation(String location) {
    }

    record Compensation(
        String compensationTierSummary,
        String scrapeableCompensationSalarySummary
    ) {
    }
}
