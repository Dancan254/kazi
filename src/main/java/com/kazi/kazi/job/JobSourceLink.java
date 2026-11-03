package com.kazi.kazi.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "job_source")
public class JobSourceLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false, length = 50)
    private String sourceName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String sourceJobId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> rawData;

    @Column(nullable = false)
    private Instant firstSeenAt;

    @Column(nullable = false)
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean active;

    protected JobSourceLink() {
    }

    JobSourceLink(
        Job job,
        String sourceName,
        String sourceUrl,
        String sourceJobId,
        Map<String, Object> rawData,
        Instant now
    ) {
        this.job = job;
        this.sourceName = sourceName;
        this.sourceUrl = sourceUrl;
        this.sourceJobId = sourceJobId;
        this.rawData = rawData;
        this.firstSeenAt = now;
        this.lastSeenAt = now;
        this.active = true;
    }

    void seenAgain(
        Job job,
        String sourceUrl,
        String sourceJobId,
        Map<String, Object> rawData,
        Instant now
    ) {
        this.job = job;
        this.sourceUrl = sourceUrl;
        this.sourceJobId = sourceJobId;
        this.rawData = rawData;
        this.lastSeenAt = now;
        this.active = true;
    }

    void deactivate() {
        this.active = false;
    }

    public UUID getId() {
        return id;
    }

    Job getJob() {
        return job;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSourceJobId() {
        return sourceJobId;
    }

    public Map<String, Object> getRawData() {
        return rawData;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isActive() {
        return active;
    }
}
