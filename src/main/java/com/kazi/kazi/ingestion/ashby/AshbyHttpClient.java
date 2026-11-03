package com.kazi.kazi.ingestion.ashby;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface AshbyHttpClient {

    @GetExchange("/{board}")
    AshbyResponse getJobs(@PathVariable String board, @RequestParam boolean includeCompensation);
}
