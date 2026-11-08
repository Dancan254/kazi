package com.kazi.kazi.job;

import com.kazi.kazi.ingestion.normalization.HtmlTextNormalizer;
import com.kazi.kazi.ingestion.normalization.NormalizedJob;
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
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String deterministicKey;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 200)
    private String company;

    @Column(length = 300)
    private String location;

    @Column(nullable = false)
    private boolean remote;

    private Integer salaryMin;
    private Integer salaryMax;

    @Column(length = 3)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 20)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private NormalizedJob.EmploymentType employmentType;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Job() {
    }

    Job(NormalizedJob job, String deterministicKey, String location, Instant now) {
        this.deterministicKey = deterministicKey;
        this.title = job.title();
        this.company = job.company();
        this.location = location;
        this.remote = job.remote();
        this.salaryMin = job.salaryMin();
        this.salaryMax = job.salaryMax();
        this.currency = job.currency();
        this.description = job.description();
        this.employmentType = job.employmentType();
        this.status = "active";
        this.createdAt = now;
        this.updatedAt = now;
    }

    void merge(NormalizedJob job, String location, Instant now) {
        description = HtmlTextNormalizer.toPlainText(description);
        String candidateDescription = job.description();
        if (length(candidateDescription) > length(description)) {
            description = candidateDescription;
        }
        salaryMin = minimum(salaryMin, job.salaryMin());
        salaryMax = maximum(salaryMax, job.salaryMax());
        if (job.currency() != null) {
            currency = job.currency();
        }
        if (specificity(location) > specificity(this.location)) {
            this.location = location;
        }
        status = "active";
        updatedAt = now;
    }

    void deactivate(Instant now) {
        status = "inactive";
        updatedAt = now;
    }

    private Integer minimum(Integer current, Integer candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate == null ? current : Math.min(current, candidate);
    }

    private Integer maximum(Integer current, Integer candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate == null ? current : Math.max(current, candidate);
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private int specificity(String value) {
        return value == null || value.isBlank() ? 0 : value.split(",").length;
    }

    public UUID getId() {
        return id;
    }

    public String getDeterministicKey() {
        return deterministicKey;
    }

    public String getTitle() {
        return title;
    }

    public String getCompany() {
        return company;
    }

    public String getLocation() {
        return location;
    }

    public boolean isRemote() {
        return remote;
    }

    public Integer getSalaryMin() {
        return salaryMin;
    }

    public Integer getSalaryMax() {
        return salaryMax;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public NormalizedJob.EmploymentType getEmploymentType() {
        return employmentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
