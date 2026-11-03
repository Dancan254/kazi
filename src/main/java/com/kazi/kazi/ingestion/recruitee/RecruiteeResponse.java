package com.kazi.kazi.ingestion.recruitee;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RecruiteeResponse(List<Offer> offers) {

    public record Offer(
        Long position,
        String guid,
        String title,
        String location,
        String city,
        Boolean remote,
        Boolean hybrid,
        @JsonProperty("employment_type_code") String employmentTypeCode,
        @JsonProperty("published_at") String publishedAt,
        @JsonProperty("careers_url") String careersUrl,
        String description,
        String requirements,
        Salary salary,
        List<OfferLocation> locations
    ) {
    }

    public record Salary(Integer min, Integer max, String period, String currency) {
    }

    public record OfferLocation(String name, String city, String country, String state) {
    }
}
