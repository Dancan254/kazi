package com.kazi.kazi.ingestion.discovery;

import java.net.URI;
import org.springframework.web.service.annotation.GetExchange;

interface DiscoveryHttpClient {

    @GetExchange
    String getPage(URI uri);
}
