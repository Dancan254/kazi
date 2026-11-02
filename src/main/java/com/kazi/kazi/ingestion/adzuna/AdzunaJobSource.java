package com.kazi.kazi.ingestion.adzuna;

import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class AdzunaJobSource implements JobSource {

    private final AdzunaHttpClient httpClient;
    private final AdzunaProperties properties;

    AdzunaJobSource(AdzunaHttpClient httpClient, AdzunaProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "adzuna";
    }

    @Override
    @CircuitBreaker(name = "adzuna", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${adzuna.retry.max-retries:2}",
        delayString = "${adzuna.retry.delay-ms:2000}",
        jitterString = "${adzuna.retry.jitter-ms:500}",
        multiplierString = "${adzuna.retry.multiplier:2}",
        maxDelayString = "${adzuna.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(Instant lastRun) {
        List<RawJob> jobs = new ArrayList<>();
        int page = 1;

        while (true) {
            AdzunaResponse response = fetchPage(page);
            if (response == null || response.results() == null || response.results().isEmpty()) {
                return jobs;
            }

            response.results().stream()
                .filter(job -> lastRun == null || job.created() == null || !job.created().isBefore(lastRun))
                .map(this::toRawJob)
                .forEach(jobs::add);

            if (response.results().size() < properties.resultsPerPage()) {
                return jobs;
            }
            page++;
        }
    }

    private AdzunaResponse fetchPage(int page) {
        return httpClient.getJobs(
            properties.country(),
            page,
            properties.appId(),
            properties.appKey(),
            properties.resultsPerPage(),
            "software engineer",
            "date"
        );
    }

    private RawJob toRawJob(AdzunaResponse.AdzunaJob job) {
        return new RawJob(
            sourceName(),
            job.id(),
            job.redirectUrl(),
            job.title(),
            job.company() == null ? null : job.company().displayName(),
            job.location() == null ? null : job.location().displayName(),
            salary(job),
            job.contractTime() == null ? job.contractType() : job.contractTime(),
            job.description(),
            job.created(),
            Map.of("adzuna", job)
        );
    }

    private String salary(AdzunaResponse.AdzunaJob job) {
        if (job.salaryMin() == null && job.salaryMax() == null) {
            return null;
        }

        var min = job.salaryMin() == null ? job.salaryMax() : job.salaryMin();
        var max = job.salaryMax() == null ? job.salaryMin() : job.salaryMax();
        String range = min.stripTrailingZeros().toPlainString();
        if (!min.equals(max)) {
            range += " - " + max.stripTrailingZeros().toPlainString();
        }
        return properties.currency() + " " + range;
    }

    private List<RawJob> unavailable(Instant lastRun, Exception exception) {
        throw new JobSourceException("Adzuna is unavailable", exception);
    }
}
