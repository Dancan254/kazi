package com.kazi.kazi.ingestion.workday;

import java.time.LocalDate;
import java.util.List;

record WorkdayJobResponse(JobPostingInfo jobPostingInfo) {

    record JobPostingInfo(
        String jobReqId,
        String title,
        String location,
        LocalDate startDate,
        String timeType,
        String jobDescription,
        String externalUrl,
        Location jobRequisitionLocation,
        List<String> additionalLocations
    ) {
    }

    record Location(String descriptor, Country country) {
    }

    record Country(String descriptor, String alpha2Code) {
    }
}
