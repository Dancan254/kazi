package com.kazi.kazi.ingestion.workable;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record WorkableAccountSource(
    WorkableJobSource jobSource,
    WorkableProperties.Account account
) implements JobSource {

    @Override
    public String sourceName() {
        return WorkableJobSource.sourceName(account);
    }

    @Override
    public List<RawJob> fetchSince(Instant lastRun) {
        return jobSource.fetchSince(account, lastRun);
    }

    @Override
    public boolean providesFullSnapshot() {
        return true;
    }
}
