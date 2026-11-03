package com.kazi.kazi.ingestion.lever;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record LeverSiteSource(
    LeverJobSource jobSource,
    LeverProperties.Site site
) implements JobSource {

    @Override
    public String sourceName() {
        return LeverJobSource.sourceName(site);
    }

    @Override
    public List<RawJob> fetchSince(Instant lastRun) {
        return jobSource.fetchSince(site, lastRun);
    }
}
