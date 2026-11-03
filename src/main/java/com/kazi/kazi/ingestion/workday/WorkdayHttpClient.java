package com.kazi.kazi.ingestion.workday;

import java.net.URI;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

interface WorkdayHttpClient {

    @PostExchange
    WorkdayJobsResponse getJobs(URI uri, @RequestBody WorkdaySearchRequest request);

    @GetExchange
    WorkdayJobResponse getJob(URI uri);
}
