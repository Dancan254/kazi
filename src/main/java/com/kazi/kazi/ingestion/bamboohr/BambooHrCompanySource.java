package com.kazi.kazi.ingestion.bamboohr;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record BambooHrCompanySource(
    BambooHrJobSource jobSource,
    BambooHrProperties.Company company
) implements JobSource {

    @Override
    public String sourceName() {
        return BambooHrJobSource.sourceName(company);
    }

    @Override
    public List<RawJob> fetchSince(Instant lastRun) {
        return jobSource.fetchSince(company, lastRun);
    }

    @Override
    public boolean providesFullSnapshot() {
        return true;
    }
}
