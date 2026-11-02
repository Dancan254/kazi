package com.kazi.kazi.ingestion.normalization;

import com.kazi.kazi.ingestion.source.RawJob;

public record NormalizedJob(
    RawJob rawJob,
    String title,
    String company,
    String city,
    String region,
    String country,
    boolean remote,
    Integer salaryMin,
    Integer salaryMax,
    String currency,
    EmploymentType employmentType
) {

    public enum EmploymentType {
        FULL_TIME,
        PART_TIME,
        CONTRACT,
        INTERNSHIP
    }
}
