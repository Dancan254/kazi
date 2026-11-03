package com.kazi.kazi.ingestion.greenhouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

record GreenhouseResponse(List<GreenhouseJob> jobs) {

    record GreenhouseJob(
        Long id,
        String title,
        Location location,
        String content,
        @JsonProperty("absolute_url") String absoluteUrl,
        @JsonProperty("updated_at") Instant updatedAt
    ) {
    }

    record Location(String name) {
    }
}
