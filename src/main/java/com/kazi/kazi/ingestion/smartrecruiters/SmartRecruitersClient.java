package com.kazi.kazi.ingestion.smartrecruiters;

import com.kazi.kazi.ingestion.source.JobSourceException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
class SmartRecruitersClient {

    private final SmartRecruitersHttpClient httpClient;

    SmartRecruitersClient(SmartRecruitersHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "smartrecruiters", fallbackMethod = "postingsUnavailable")
    @Retryable(
        maxRetriesString = "${smartrecruiters.retry.max-retries:2}",
        delayString = "${smartrecruiters.retry.delay-ms:2000}",
        jitterString = "${smartrecruiters.retry.jitter-ms:500}",
        multiplierString = "${smartrecruiters.retry.multiplier:2}",
        maxDelayString = "${smartrecruiters.retry.max-delay-ms:30000}"
    )
    SmartRecruitersPostingsResponse getPostings(String identifier, int limit, int offset) {
        return httpClient.getPostings(identifier, limit, offset);
    }

    @CircuitBreaker(name = "smartrecruiters", fallbackMethod = "postingUnavailable")
    @Retryable(
        maxRetriesString = "${smartrecruiters.retry.max-retries:2}",
        delayString = "${smartrecruiters.retry.delay-ms:2000}",
        jitterString = "${smartrecruiters.retry.jitter-ms:500}",
        multiplierString = "${smartrecruiters.retry.multiplier:2}",
        maxDelayString = "${smartrecruiters.retry.max-delay-ms:30000}"
    )
    SmartRecruitersPostingResponse getPosting(String identifier, String id) {
        return httpClient.getPosting(identifier, id);
    }

    private SmartRecruitersPostingsResponse postingsUnavailable(
        String identifier,
        int limit,
        int offset,
        Exception exception
    ) {
        throw unavailable(identifier, exception);
    }

    private SmartRecruitersPostingResponse postingUnavailable(
        String identifier,
        String id,
        Exception exception
    ) {
        throw unavailable(identifier, exception);
    }

    private JobSourceException unavailable(String identifier, Exception exception) {
        return new JobSourceException(
            "SmartRecruiters company " + identifier + " is unavailable: " + exception,
            exception
        );
    }
}
