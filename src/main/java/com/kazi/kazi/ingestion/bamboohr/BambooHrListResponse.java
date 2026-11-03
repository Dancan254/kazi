package com.kazi.kazi.ingestion.bamboohr;

import java.util.List;

public record BambooHrListResponse(Meta meta, List<BambooHrJob> result) {

    public record Meta(Integer totalCount) {
    }

    public record BambooHrJob(
        String id,
        String jobOpeningName,
        String employmentStatusLabel,
        String employmentType,
        Location location,
        Location atsLocation,
        Boolean isRemote,
        String locationType
    ) {
    }

    public record Location(
        String city,
        String state,
        String province,
        String country,
        String addressCountry
    ) {
    }
}
