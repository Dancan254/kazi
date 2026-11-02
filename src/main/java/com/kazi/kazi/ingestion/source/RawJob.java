package com.kazi.kazi.ingestion.source;

import java.time.Instant;
import java.util.Map;

public record RawJob(
    String sourceName,
    String sourceJobId,
    String sourceUrl,
    String title,
    String company,
    String location,
    String salary,
    String employmentType,
    String description,
    Instant postedAt,
    Map<String, Object> rawData
) {
}
