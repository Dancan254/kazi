package com.kazi.kazi.ingestion.source;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface JobSource {

    String sourceName();

    List<RawJob> fetchSince(Instant lastRun);

    default Duration pollInterval() {
        return Duration.ofHours(6);
    }
}
