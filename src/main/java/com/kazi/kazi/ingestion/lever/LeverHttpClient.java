package com.kazi.kazi.ingestion.lever;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface LeverHttpClient {

    @GetExchange("/{site}")
    List<LeverPosting> getJobs(
        @PathVariable String site,
        @RequestParam int skip,
        @RequestParam int limit,
        @RequestParam String mode
    );
}
