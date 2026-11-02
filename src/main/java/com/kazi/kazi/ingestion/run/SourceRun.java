package com.kazi.kazi.ingestion.run;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "source_run")
public class SourceRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String sourceName;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceRunStatus status;

    @Column(nullable = false)
    private int jobsFetched;

    @Column(nullable = false)
    private int jobsInserted;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    protected SourceRun() {
    }

    public SourceRun(String sourceName, Instant startedAt) {
        this.sourceName = sourceName;
        this.startedAt = startedAt;
        this.status = SourceRunStatus.RUNNING;
    }

    public void complete(Instant finishedAt, int jobsFetched) {
        this.finishedAt = finishedAt;
        this.jobsFetched = jobsFetched;
        this.jobsInserted = 0;
        this.status = SourceRunStatus.COMPLETED;
    }

    public void fail(Instant finishedAt, String errorMessage) {
        this.finishedAt = finishedAt;
        this.errorMessage = errorMessage;
        this.status = SourceRunStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public String getSourceName() {
        return sourceName;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public SourceRunStatus getStatus() {
        return status;
    }

    public int getJobsFetched() {
        return jobsFetched;
    }

    public int getJobsInserted() {
        return jobsInserted;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
