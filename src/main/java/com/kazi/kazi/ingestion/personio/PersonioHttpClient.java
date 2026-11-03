package com.kazi.kazi.ingestion.personio;

import java.net.URI;
import org.springframework.web.service.annotation.GetExchange;

interface PersonioHttpClient {

    @GetExchange
    String getPositions(URI uri);
}
