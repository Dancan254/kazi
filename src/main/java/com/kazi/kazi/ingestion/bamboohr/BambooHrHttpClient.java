package com.kazi.kazi.ingestion.bamboohr;

import java.net.URI;
import org.springframework.web.service.annotation.GetExchange;

interface BambooHrHttpClient {

    @GetExchange
    BambooHrListResponse getJobs(URI uri);

    @GetExchange
    BambooHrDetailResponse getJob(URI uri);
}
