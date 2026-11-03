package com.kazi.kazi.ingestion.smartrecruiters;

import java.time.Instant;

public record SmartRecruitersPostingResponse(
    String id,
    String name,
    SmartRecruitersPostingsResponse.Company company,
    SmartRecruitersPostingsResponse.Location location,
    SmartRecruitersPostingsResponse.EmploymentType typeOfEmployment,
    Instant releasedDate,
    String postingUrl,
    String applyUrl,
    JobAd jobAd
) {
    public record JobAd(Sections sections) {
    }

    public record Sections(
        Section companyDescription,
        Section jobDescription,
        Section qualifications,
        Section additionalInformation
    ) {
    }

    public record Section(String title, String text) {
    }
}
