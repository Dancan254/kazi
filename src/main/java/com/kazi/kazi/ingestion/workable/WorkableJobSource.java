package com.kazi.kazi.ingestion.workable;

import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class WorkableJobSource {

    private final WorkableHttpClient httpClient;

    WorkableJobSource(WorkableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "workable", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${workable.retry.max-retries:2}",
        delayString = "${workable.retry.delay-ms:2000}",
        jitterString = "${workable.retry.jitter-ms:500}",
        multiplierString = "${workable.retry.multiplier:2}",
        maxDelayString = "${workable.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(WorkableProperties.Account account, Instant lastRun) {
        WorkableResponse response = httpClient.getJobs(account.account(), true);
        if (response == null || response.jobs() == null) {
            return List.of();
        }
        return response.jobs().stream()
            .map(job -> toRawJob(account, job))
            .toList();
    }

    static String sourceName(WorkableProperties.Account account) {
        return "workable:" + account.account();
    }

    private RawJob toRawJob(WorkableProperties.Account account, WorkableResponse.WorkableJob job) {
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("title", job.title());
        rawData.put("shortcode", job.shortcode());
        rawData.put("employmentType", job.employmentType());
        rawData.put("telecommuting", job.telecommuting());
        rawData.put("city", job.city());
        rawData.put("state", job.state());
        rawData.put("country", job.country());
        rawData.put("locations", job.locations());
        rawData.put("publishedOn", job.publishedOn() == null ? null : job.publishedOn().toString());
        rawData.put("applicationUrl", job.applicationUrl());
        rawData.put("url", job.url());
        rawData.put("description", job.description());

        return new RawJob(
            sourceName(account),
            job.shortcode(),
            hasText(job.applicationUrl()) ? job.applicationUrl() : job.url(),
            job.title(),
            account.company(),
            location(job),
            null,
            job.employmentType(),
            job.description(),
            job.publishedOn() == null ? null : job.publishedOn().atStartOfDay().toInstant(ZoneOffset.UTC),
            Map.of("workable", rawData)
        );
    }

    private String location(WorkableResponse.WorkableJob job) {
        String value = job.locations() == null || job.locations().isEmpty()
            ? Stream.of(job.city(), job.state(), job.country())
                .filter(Objects::nonNull)
                .filter(this::hasText)
                .collect(Collectors.joining(", "))
            : job.locations().stream()
                .map(location -> Stream.of(location.city(), location.region(), location.country())
                    .filter(Objects::nonNull)
                    .filter(this::hasText)
                    .collect(Collectors.joining(", ")))
                .filter(location -> !location.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        if (!Boolean.TRUE.equals(job.telecommuting())) {
            return value;
        }
        if (value.isBlank()) {
            return "Remote";
        }
        return value.toLowerCase(Locale.ROOT).contains("remote") ? value : value + ", Remote";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<RawJob> unavailable(
        WorkableProperties.Account account,
        Instant lastRun,
        Exception exception
    ) {
        throw new JobSourceException(
            "Workable account " + account.account() + " is unavailable: " + exception,
            exception
        );
    }
}
