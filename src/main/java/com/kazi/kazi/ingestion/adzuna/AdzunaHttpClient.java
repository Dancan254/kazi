package com.kazi.kazi.ingestion.adzuna;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/jobs")
interface AdzunaHttpClient {

    @GetExchange("/{country}/search/{page}")
    AdzunaResponse getJobs(
        @PathVariable String country,
        @PathVariable int page,
        @RequestParam("app_id") String appId,
        @RequestParam("app_key") String appKey,
        @RequestParam("results_per_page") int resultsPerPage,
        @RequestParam String what,
        @RequestParam("sort_by") String sortBy
    );
}
