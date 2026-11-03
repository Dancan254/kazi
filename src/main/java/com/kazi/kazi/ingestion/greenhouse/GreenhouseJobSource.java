package com.kazi.kazi.ingestion.greenhouse;

import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class GreenhouseJobSource {

    private final GreenhouseHttpClient httpClient;

    GreenhouseJobSource(GreenhouseHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "greenhouse", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${greenhouse.retry.max-retries:2}",
        delayString = "${greenhouse.retry.delay-ms:2000}",
        jitterString = "${greenhouse.retry.jitter-ms:500}",
        multiplierString = "${greenhouse.retry.multiplier:2}",
        maxDelayString = "${greenhouse.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(GreenhouseProperties.Board board, Instant lastRun) {
        GreenhouseResponse response = httpClient.getJobs(board.token(), true);
        if (response == null || response.jobs() == null) {
            return List.of();
        }

        return response.jobs().stream()
            .filter(job -> lastRun == null || job.updatedAt() == null || !job.updatedAt().isBefore(lastRun))
            .map(job -> toRawJob(board, job))
            .toList();
    }

    static String sourceName(GreenhouseProperties.Board board) {
        return "greenhouse:" + board.token();
    }

    private RawJob toRawJob(GreenhouseProperties.Board board, GreenhouseResponse.GreenhouseJob job) {
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("id", job.id());
        rawData.put("title", job.title());
        rawData.put("location", job.location() == null ? null : job.location().name());
        rawData.put("content", job.content());
        rawData.put("absolute_url", job.absoluteUrl());
        rawData.put("updated_at", job.updatedAt() == null ? null : job.updatedAt().toString());

        return new RawJob(
            sourceName(board),
            String.valueOf(job.id()),
            job.absoluteUrl(),
            job.title(),
            board.company(),
            job.location() == null ? null : job.location().name(),
            null,
            null,
            job.content(),
            null,
            Map.of("greenhouse", rawData)
        );
    }

    private List<RawJob> unavailable(GreenhouseProperties.Board board, Instant lastRun, Exception exception) {
        throw new JobSourceException("Greenhouse board " + board.token() + " is unavailable: " + exception, exception);
    }
}
