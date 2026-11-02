package com.kazi.kazi.job;

import com.kazi.kazi.ingestion.normalization.NormalizedJob;
import com.kazi.kazi.ingestion.source.RawJob;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobSourceLinkRepository sourceLinkRepository;
    private final Clock clock;

    JobService(JobRepository jobRepository, JobSourceLinkRepository sourceLinkRepository, Clock clock) {
        this.jobRepository = jobRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.clock = clock;
    }

    public record SavedJob(UUID id, boolean inserted) {
    }

    @Transactional
    public SavedJob save(NormalizedJob normalizedJob) {
        String deterministicKey = deterministicKey(normalizedJob);
        String location = location(normalizedJob);
        Instant now = clock.instant();
        var existingJob = jobRepository.findByDeterministicKey(deterministicKey);
        boolean inserted = existingJob.isEmpty();

        Job job = existingJob
            .map(existing -> {
                existing.merge(normalizedJob, location, now);
                return existing;
            })
            .orElseGet(() -> new Job(normalizedJob, deterministicKey, location, now));
        job = jobRepository.save(job);

        RawJob rawJob = normalizedJob.rawJob();
        Map<String, Object> rawData = rawJob.rawData() == null ? Map.of() : rawJob.rawData();
        var sourceLink = sourceLinkRepository.findByJob_IdAndSourceName(job.getId(), rawJob.sourceName());
        if (sourceLink.isPresent()) {
            JobSourceLink link = sourceLink.get();
            link.seenAgain(rawJob.sourceUrl(), rawJob.sourceJobId(), rawData, now);
            sourceLinkRepository.save(link);
        } else {
            sourceLinkRepository.save(new JobSourceLink(
                job,
                rawJob.sourceName(),
                rawJob.sourceUrl(),
                rawJob.sourceJobId(),
                rawData,
                now
            ));
        }
        return new SavedJob(job.getId(), inserted);
    }

    private String deterministicKey(NormalizedJob job) {
        String locationKey = Stream.of(
                job.city(),
                job.region(),
                job.country(),
                Boolean.toString(job.remote())
            )
            .map(value -> value == null ? "" : value)
            .reduce((left, right) -> left + "|" + right)
            .orElse("");
        String value = job.company() + "|" + job.title() + "|" + locationKey;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String location(NormalizedJob job) {
        String location = Stream.of(job.city(), job.region(), job.country())
            .filter(Objects::nonNull)
            .reduce((left, right) -> left + ", " + right)
            .orElse(null);
        return location == null || location.isBlank() ? null : location;
    }
}
