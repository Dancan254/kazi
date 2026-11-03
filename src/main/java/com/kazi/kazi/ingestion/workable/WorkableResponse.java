package com.kazi.kazi.ingestion.workable;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

public record WorkableResponse(String name, List<WorkableJob> jobs) {

    public record WorkableJob(
        String title,
        String shortcode,
        @JsonProperty("employment_type") String employmentType,
        Boolean telecommuting,
        String city,
        String state,
        String country,
        @JsonProperty("published_on") LocalDate publishedOn,
        @JsonProperty("application_url") String applicationUrl,
        String url,
        String description,
        List<WorkableLocation> locations
    ) {
    }

    public record WorkableLocation(String country, String city, String region) {
    }
}
