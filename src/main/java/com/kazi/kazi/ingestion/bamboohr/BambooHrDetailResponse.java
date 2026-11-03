package com.kazi.kazi.ingestion.bamboohr;

import java.time.LocalDate;

public record BambooHrDetailResponse(Result result) {

    public record Result(JobOpening jobOpening) {
    }

    public record JobOpening(
        String jobOpeningShareUrl,
        String jobOpeningName,
        String employmentStatusLabel,
        String employmentType,
        BambooHrListResponse.Location location,
        BambooHrListResponse.Location atsLocation,
        String description,
        String compensation,
        LocalDate datePosted,
        String locationType
    ) {
    }
}
