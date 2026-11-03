package com.kazi.kazi.ingestion.greenhouse;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface GreenhouseHttpClient {

    @GetExchange("/{boardToken}/jobs")
    GreenhouseResponse getJobs(@PathVariable String boardToken, @RequestParam boolean content);
}
