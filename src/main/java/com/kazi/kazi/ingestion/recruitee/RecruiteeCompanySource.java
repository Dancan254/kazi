package com.kazi.kazi.ingestion.recruitee;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record RecruiteeCompanySource(
    RecruiteeJobSource jobSource,
    RecruiteeProperties.Company company
) implements JobSource {

    @Override
    public String sourceName() {
        return RecruiteeJobSource.sourceName(company);
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
