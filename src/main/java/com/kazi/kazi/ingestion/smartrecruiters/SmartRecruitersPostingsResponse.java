package com.kazi.kazi.ingestion.smartrecruiters;

import java.time.Instant;
import java.util.List;

public record SmartRecruitersPostingsResponse(
    Integer offset,
    Integer limit,
    Integer totalFound,
    List<Posting> content
) {
    public record Posting(
        String id,
        String name,
        Company company,
        Location location,
        EmploymentType typeOfEmployment,
        Instant releasedDate
    ) {
    }

    public record Company(String identifier, String name) {
    }

    public record Location(
        String city,
        String region,
        String country,
        Boolean remote,
        Boolean hybrid,
        String fullLocation
    ) {
    }

    public record EmploymentType(String id, String label) {
    }
}
