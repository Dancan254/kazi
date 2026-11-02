package com.kazi.kazi.ingestion.adzuna;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

record AdzunaResponse(List<AdzunaJob> results) {

    record AdzunaJob(
        String id,
        String title,
        Company company,
        Location location,
        @JsonProperty("salary_min") BigDecimal salaryMin,
        @JsonProperty("salary_max") BigDecimal salaryMax,
        @JsonProperty("contract_type") String contractType,
        @JsonProperty("contract_time") String contractTime,
        String description,
        @JsonProperty("redirect_url") String redirectUrl,
        Instant created
    ) {
    }

    record Company(@JsonProperty("display_name") String displayName) {
    }

    record Location(@JsonProperty("display_name") String displayName) {
    }
}
