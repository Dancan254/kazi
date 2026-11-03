package com.kazi.kazi.ingestion.smartrecruiters;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record SmartRecruitersCompanySource(
    SmartRecruitersJobSource jobSource,
    SmartRecruitersProperties.Company company
) implements JobSource {

    @Override
    public String sourceName() {
        return SmartRecruitersJobSource.sourceName(company);
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
