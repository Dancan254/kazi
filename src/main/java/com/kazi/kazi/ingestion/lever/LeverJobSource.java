package com.kazi.kazi.ingestion.lever;

import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class LeverJobSource {

    private final LeverHttpClient httpClient;
    private final LeverProperties properties;

    LeverJobSource(LeverHttpClient httpClient, LeverProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @CircuitBreaker(name = "lever", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${lever.retry.max-retries:2}",
        delayString = "${lever.retry.delay-ms:2000}",
        jitterString = "${lever.retry.jitter-ms:500}",
        multiplierString = "${lever.retry.multiplier:2}",
        maxDelayString = "${lever.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(LeverProperties.Site site, Instant lastRun) {
        List<RawJob> jobs = new ArrayList<>();
        int skip = 0;

        while (true) {
            List<LeverPosting> response = httpClient.getJobs(site.site(), skip, properties.pageSize(), "json");
            if (response == null || response.isEmpty()) {
                return jobs;
            }

            response.stream()
                .filter(job -> lastRun == null || job.createdAt() == null
                    || !Instant.ofEpochMilli(job.createdAt()).isBefore(lastRun))
                .map(job -> toRawJob(site, job))
                .forEach(jobs::add);

            if (response.size() < properties.pageSize()) {
                return jobs;
            }
            skip += response.size();
        }
    }

    static String sourceName(LeverProperties.Site site) {
        return "lever:" + site.site();
    }

    private RawJob toRawJob(LeverProperties.Site site, LeverPosting job) {
        return new RawJob(
            sourceName(site),
            job.id(),
            job.applyUrl() == null ? job.hostedUrl() : job.applyUrl(),
            job.text(),
            site.company(),
            location(job),
            salary(job),
            job.categories() == null ? null : job.categories().commitment(),
            description(job),
            job.createdAt() == null ? null : Instant.ofEpochMilli(job.createdAt()),
            Map.of("lever", job)
        );
    }

    private String location(LeverPosting job) {
        String location = job.categories() == null ? null : job.categories().location();
        if ("remote".equals(job.workplaceType())) {
            return location == null || location.isBlank() ? "Remote" : location + ", Remote";
        }
        return location;
    }

    private String salary(LeverPosting job) {
        if (job.salaryRange() == null) {
            return job.salaryDescriptionPlain();
        }

        var range = job.salaryRange();
        if (range.min() == null && range.max() == null) {
            return job.salaryDescriptionPlain();
        }
        var min = range.min() == null ? range.max() : range.min();
        var max = range.max() == null ? range.min() : range.max();
        String value = range.currency() + " " + min.stripTrailingZeros().toPlainString();
        if (!min.equals(max)) {
            value += " - " + max.stripTrailingZeros().toPlainString();
        }
        return range.interval() == null ? value : value + " " + range.interval();
    }

    private String description(LeverPosting job) {
        Stream<String> lists = job.lists() == null ? Stream.empty() : job.lists().stream()
            .map(list -> list.text() + "\n" + list.content());
        return Stream.concat(Stream.of(job.description(), job.additional()), lists)
            .filter(Objects::nonNull)
            .filter(section -> !section.isBlank())
            .collect(Collectors.joining("\n\n"));
    }

    private List<RawJob> unavailable(LeverProperties.Site site, Instant lastRun, Exception exception) {
        throw new JobSourceException("Lever site " + site.site() + " is unavailable: " + exception, exception);
    }
}
