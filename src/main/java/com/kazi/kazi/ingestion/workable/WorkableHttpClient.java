package com.kazi.kazi.ingestion.workable;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface WorkableHttpClient {

    @GetExchange("/{account}")
    WorkableResponse getJobs(@PathVariable String account, @RequestParam boolean details);
}
