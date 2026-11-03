package com.kazi.kazi.ingestion.workday;

import java.util.List;

record WorkdayJobsResponse(int total, List<JobPosting> jobPostings) {

    record JobPosting(
        String title,
        String externalPath,
        String locationsText,
        String postedOn,
        String remoteType,
        List<String> bulletFields
    ) {
    }
}
