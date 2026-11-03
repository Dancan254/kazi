package com.kazi.kazi.ingestion.discovery;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

record RoutedJobSource(String sourceName, Function<Instant, List<RawJob>> fetch) implements JobSource {

    @Override
    public List<RawJob> fetchSince(Instant lastRun) {
        return fetch.apply(lastRun);
    }

    @Override
    public boolean providesFullSnapshot() {
        return true;
    }
}
