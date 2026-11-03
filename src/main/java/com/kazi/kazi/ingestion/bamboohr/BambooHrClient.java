package com.kazi.kazi.ingestion.bamboohr;

import com.kazi.kazi.ingestion.source.JobSourceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
class BambooHrClient {

    private final BambooHrHttpClient httpClient;

    BambooHrClient(BambooHrHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "bamboohr", fallbackMethod = "jobsUnavailable")
    @Retryable(
        maxRetriesString = "${bamboohr.retry.max-retries:2}",
        delayString = "${bamboohr.retry.delay-ms:2000}",
        jitterString = "${bamboohr.retry.jitter-ms:500}",
        multiplierString = "${bamboohr.retry.multiplier:2}",
        maxDelayString = "${bamboohr.retry.max-delay-ms:30000}"
    )
    BambooHrListResponse getJobs(URI uri) {
        return httpClient.getJobs(uri);
    }

    @CircuitBreaker(name = "bamboohr", fallbackMethod = "jobUnavailable")
    @Retryable(
        maxRetriesString = "${bamboohr.retry.max-retries:2}",
        delayString = "${bamboohr.retry.delay-ms:2000}",
        jitterString = "${bamboohr.retry.jitter-ms:500}",
        multiplierString = "${bamboohr.retry.multiplier:2}",
        maxDelayString = "${bamboohr.retry.max-delay-ms:30000}"
    )
    BambooHrDetailResponse getJob(URI uri) {
        return httpClient.getJob(uri);
    }

    private BambooHrListResponse jobsUnavailable(URI uri, Exception exception) {
        throw unavailable(uri, exception);
    }

    private BambooHrDetailResponse jobUnavailable(URI uri, Exception exception) {
        throw unavailable(uri, exception);
    }

    private JobSourceException unavailable(URI uri, Exception exception) {
        return new JobSourceException("BambooHR endpoint " + uri + " is unavailable: " + exception, exception);
    }
}
