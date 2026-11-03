package com.kazi.kazi.ingestion.personio;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record PersonioCompanySource(
    PersonioJobSource jobSource,
    PersonioProperties.Company company
) implements JobSource {

    @Override
    public String sourceName() {
        return PersonioJobSource.sourceName(company);
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
