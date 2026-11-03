package com.kazi.kazi.ingestion.workday;

import com.kazi.kazi.ingestion.source.JobSourceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
class WorkdayClient {

    private final WorkdayHttpClient httpClient;

    WorkdayClient(WorkdayHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "workday", fallbackMethod = "jobsUnavailable")
    @Retryable(
        maxRetriesString = "${workday.retry.max-retries:2}",
        delayString = "${workday.retry.delay-ms:2000}",
        jitterString = "${workday.retry.jitter-ms:500}",
        multiplierString = "${workday.retry.multiplier:2}",
        maxDelayString = "${workday.retry.max-delay-ms:30000}"
    )
    WorkdayJobsResponse getJobs(URI uri, WorkdaySearchRequest request) {
        return httpClient.getJobs(uri, request);
    }

    @CircuitBreaker(name = "workday", fallbackMethod = "jobUnavailable")
    @Retryable(
        maxRetriesString = "${workday.retry.max-retries:2}",
        delayString = "${workday.retry.delay-ms:2000}",
        jitterString = "${workday.retry.jitter-ms:500}",
        multiplierString = "${workday.retry.multiplier:2}",
        maxDelayString = "${workday.retry.max-delay-ms:30000}"
    )
    WorkdayJobResponse getJob(URI uri) {
        return httpClient.getJob(uri);
    }

    private WorkdayJobsResponse jobsUnavailable(
        URI uri,
        WorkdaySearchRequest request,
        Exception exception
    ) {
        throw new JobSourceException("Workday endpoint " + uri + " is unavailable: " + exception, exception);
    }

    private WorkdayJobResponse jobUnavailable(URI uri, Exception exception) {
        throw new JobSourceException("Workday endpoint " + uri + " is unavailable: " + exception, exception);
    }
}
