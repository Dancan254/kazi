package com.kazi.kazi.ingestion.greenhouse;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.List;

record GreenhouseBoardSource(
    GreenhouseJobSource jobSource,
    GreenhouseProperties.Board board
) implements JobSource {

    @Override
    public String sourceName() {
        return GreenhouseJobSource.sourceName(board);
    }

    @Override
    public List<RawJob> fetchSince(Instant lastRun) {
        return jobSource.fetchSince(board, lastRun);
    }

    @Override
    public boolean providesFullSnapshot() {
        return true;
    }
}
