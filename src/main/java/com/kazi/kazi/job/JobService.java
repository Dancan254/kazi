package com.kazi.kazi.job;

import com.kazi.kazi.ingestion.normalization.NormalizedJob;
import com.kazi.kazi.ingestion.source.RawJob;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        var existingJob = jobRepository.findByDeterministicKey(deterministicKey)
            .or(() -> fuzzyMatch(normalizedJob, location));
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
        var sourceLink = sourceLinkRepository.findBySourceNameAndSourceJobId(
            rawJob.sourceName(),
            rawJob.sourceJobId()
        );
        if (sourceLink.isPresent()) {
            JobSourceLink link = sourceLink.get();
            link.seenAgain(job, rawJob.sourceUrl(), rawJob.sourceJobId(), rawData, now);
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

    @Transactional
    public void deactivateMissing(String sourceName, Set<String> sourceJobIds) {
        List<JobSourceLink> activeLinks = sourceLinkRepository.findBySourceNameAndActiveTrue(sourceName);
        for (JobSourceLink link : activeLinks) {
            if (sourceJobIds.contains(link.getSourceJobId())) {
                continue;
            }

            link.deactivate();
            sourceLinkRepository.save(link);
            Job job = link.getJob();
            if (!sourceLinkRepository.existsByJob_IdAndActiveTrue(job.getId())) {
                job.deactivate(clock.instant());
                jobRepository.save(job);
            }
        }
    }

    private java.util.Optional<Job> fuzzyMatch(NormalizedJob candidate, String location) {
        return jobRepository.findTop50ByCompanyOrderByUpdatedAtDesc(candidate.company()).stream()
            .filter(existing -> JobSimilarity.isDuplicate(candidate, existing, location))
            .findFirst();
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
