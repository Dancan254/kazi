package com.kazi.kazi.ingestion.recruitee;

import java.net.URI;
import org.springframework.web.service.annotation.GetExchange;

interface RecruiteeHttpClient {

    @GetExchange
    RecruiteeResponse getOffers(URI uri);
}
