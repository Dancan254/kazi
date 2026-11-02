package com.kazi.kazi.ingestion;

import com.kazi.kazi.ingestion.normalization.JobNormalizer;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

    private final SourceRunRepository sourceRunRepository;
    private final JobNormalizer jobNormalizer;
    private final Clock clock;

    IngestionService(SourceRunRepository sourceRunRepository, JobNormalizer jobNormalizer, Clock clock) {
        this.sourceRunRepository = sourceRunRepository;
        this.jobNormalizer = jobNormalizer;
        this.clock = clock;
    }

    public List<SourceRun> ingestAll(List<JobSource> sources) {
        return sources.stream().map(this::ingest).toList();
    }

    public SourceRun ingest(JobSource source) {
        SourceRun run = sourceRunRepository.save(new SourceRun(source.sourceName(), clock.instant()));
        try {
            Instant lastRun = sourceRunRepository
                .findFirstBySourceNameAndStatusOrderByFinishedAtDesc(
                    source.sourceName(),
                    SourceRunStatus.COMPLETED
                )
                .map(SourceRun::getFinishedAt)
                .orElse(null);
            List<RawJob> jobs = source.fetchSince(lastRun);
            jobs.forEach(jobNormalizer::normalize);
            run.complete(clock.instant(), jobs.size());
        } catch (Exception exception) {
            run.fail(clock.instant(), exception.getMessage());
        }
        return sourceRunRepository.save(run);
    }
}
