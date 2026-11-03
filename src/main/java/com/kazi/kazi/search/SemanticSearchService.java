package com.kazi.kazi.search;

import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SemanticSearchService {

    private final JobRepository jobRepository;
    private final JobEmbeddingRepository embeddingRepository;
    private final EmbeddingModel embeddingModel;
    private final Clock clock;
    private final boolean enabled;
    private final String modelName;
    private final int dimensions;

    SemanticSearchService(
        JobRepository jobRepository,
        JobEmbeddingRepository embeddingRepository,
        EmbeddingModel embeddingModel,
        Clock clock,
        @Value("${kazi.semantic.enabled:false}") boolean enabled,
        @Value("${kazi.semantic.model:nomic-embed-text}") String modelName,
        @Value("${kazi.semantic.dimensions:768}") int dimensions
    ) {
        this.jobRepository = jobRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingModel = embeddingModel;
        this.clock = clock;
        this.enabled = enabled;
        this.modelName = modelName;
        this.dimensions = dimensions;
    }

    public void index(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        String content = content(job);
        String contentHash = hash(content);
        if (embeddingRepository.findContentHash(jobId, modelName).filter(contentHash::equals).isPresent()) {
            return;
        }
        float[] embedding = embeddingModel.embed(content);
        validateDimensions(embedding);
        embeddingRepository.save(jobId, modelName, contentHash, embedding, clock.instant());
    }

    public JobSearchService.JobSearchResult search(String query, int limit) {
        if (!enabled) {
            throw new SemanticSearchUnavailableException();
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        if (limit < 1 || limit > 50) {
            throw new IllegalArgumentException("limit must be between 1 and 50");
        }
        float[] embedding = embeddingModel.embed(query.trim());
        validateDimensions(embedding);
        var ids = embeddingRepository.search(embedding, limit);
        Map<UUID, Job> jobsById = jobRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Job::getId, Function.identity()));
        var jobs = ids.stream().map(jobsById::get).filter(java.util.Objects::nonNull).toList();
        return new JobSearchService.JobSearchResult(
            jobs.stream().map(JobSearchService.JobResponse::from).toList(),
            0,
            limit,
            jobs.size(),
            jobs.isEmpty() ? 0 : 1
        );
    }

    private String content(Job job) {
        String content = String.join(
            "\n",
            job.getTitle(),
            job.getCompany(),
            job.getLocation() == null ? "" : job.getLocation(),
            job.getDescription() == null ? "" : job.getDescription()
        );
        return content.length() > 6_000 ? content.substring(0, 6_000) : content;
    }

    private String hash(String content) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateDimensions(float[] embedding) {
        if (embedding.length != dimensions) {
            throw new IllegalStateException(
                "Embedding model returned " + embedding.length + " dimensions; expected " + dimensions
            );
        }
    }
}
