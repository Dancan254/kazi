package com.kazi.kazi.ingestion.smartrecruiters;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface SmartRecruitersHttpClient {

    @GetExchange("/{identifier}/postings")
    SmartRecruitersPostingsResponse getPostings(
        @PathVariable String identifier,
        @RequestParam int limit,
        @RequestParam int offset
    );

    @GetExchange("/{identifier}/postings/{id}")
    SmartRecruitersPostingResponse getPosting(@PathVariable String identifier, @PathVariable String id);
}
