package com.kazi.kazi.ingestion.ashby;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record AshbyOrganizationSource(
    AshbyJobSource jobSource,
    AshbyProperties.Organization organization
) implements JobSource {

    @Override
    public String sourceName() {
        return AshbyJobSource.sourceName(organization);
    }

    @Override
    public List<RawJob> fetchSince(Instant lastRun) {
        return jobSource.fetchSince(organization, lastRun);
    }
}
