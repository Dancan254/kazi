package com.kazi.kazi.ingestion.ashby;

import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class AshbyJobSource {

    private final AshbyHttpClient httpClient;

    AshbyJobSource(AshbyHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @CircuitBreaker(name = "ashby", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${ashby.retry.max-retries:2}",
        delayString = "${ashby.retry.delay-ms:2000}",
        jitterString = "${ashby.retry.jitter-ms:500}",
        multiplierString = "${ashby.retry.multiplier:2}",
        maxDelayString = "${ashby.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(AshbyProperties.Organization organization, Instant lastRun) {
        AshbyResponse response = httpClient.getJobs(organization.board(), true);
        if (response == null || response.jobs() == null) {
            return List.of();
        }

        return response.jobs().stream()
            .filter(job -> lastRun == null || job.publishedAt() == null || !job.publishedAt().isBefore(lastRun))
            .map(job -> toRawJob(organization, job))
            .toList();
    }

    static String sourceName(AshbyProperties.Organization organization) {
        return "ashby:" + organization.board();
    }

    private RawJob toRawJob(AshbyProperties.Organization organization, AshbyResponse.AshbyJob job) {
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("id", job.id());
        rawData.put("title", job.title());
        rawData.put("location", job.location());
        rawData.put("secondaryLocations", job.secondaryLocations() == null ? List.of() : job.secondaryLocations());
        rawData.put("employmentType", job.employmentType());
        rawData.put("workplaceType", job.workplaceType());
        rawData.put("descriptionHtml", job.descriptionHtml());
        rawData.put("descriptionPlain", job.descriptionPlain());
        rawData.put("publishedAt", job.publishedAt() == null ? null : job.publishedAt().toString());
        rawData.put("jobUrl", job.jobUrl());
        rawData.put("applyUrl", job.applyUrl());
        rawData.put("compensation", job.compensation());

        return new RawJob(
            sourceName(organization),
            job.id(),
            job.applyUrl() == null ? job.jobUrl() : job.applyUrl(),
            job.title(),
            organization.company(),
            location(job),
            job.compensation() == null ? null : job.compensation().scrapeableCompensationSalarySummary(),
            job.employmentType(),
            job.descriptionPlain() == null ? job.descriptionHtml() : job.descriptionPlain(),
            job.publishedAt(),
            Map.of("ashby", rawData)
        );
    }

    private String location(AshbyResponse.AshbyJob job) {
        if (!"Remote".equals(job.workplaceType())) {
            return job.location();
        }
        if (job.location() == null || job.location().isBlank()) {
            return "Remote";
        }
        return job.location().toLowerCase(Locale.ROOT).contains("remote")
            ? job.location()
            : job.location() + ", Remote";
    }

    private List<RawJob> unavailable(
        AshbyProperties.Organization organization,
        Instant lastRun,
        Exception exception
    ) {
        throw new JobSourceException(
            "Ashby board " + organization.board() + " is unavailable: " + exception,
            exception
        );
    }
}
