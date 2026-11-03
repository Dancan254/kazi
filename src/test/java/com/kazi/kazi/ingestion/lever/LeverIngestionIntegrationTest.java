package com.kazi.kazi.ingestion.lever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.lever.LeverPosting.Categories;
import com.kazi.kazi.ingestion.lever.LeverPosting.LeverList;
import com.kazi.kazi.ingestion.lever.LeverPosting.SalaryRange;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "lever.sites[0].company=Acme",
    "lever.sites[0].site=acme",
    "lever.sites[1].company=Beta",
    "lever.sites[1].site=beta",
    "lever.retry.max-retries=0"
})
class LeverIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LeverIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private LeverHttpClient httpClient;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_site_with_distinct_provenance() {
        when(httpClient.getJobs("acme", 0, 100, "json")).thenReturn(List.of(posting("job-1", "Java Engineer")));
        when(httpClient.getJobs("beta", 0, 100, "json")).thenReturn(List.of(posting("job-2", "Platform Engineer")));

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName, SourceRun::getStatus)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("lever:acme", SourceRunStatus.COMPLETED),
                org.assertj.core.groups.Tuple.tuple("lever:beta", SourceRunStatus.COMPLETED)
            );
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("lever:acme", "job-1"),
                org.assertj.core.groups.Tuple.tuple("lever:beta", "job-2")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    private LeverPosting posting(String id, String title) {
        return new LeverPosting(
            id,
            title,
            new Categories("Full Time", "Engineering", "Nairobi, Kenya", "Platform", List.of("Nairobi, Kenya")),
            "KE",
            1767225600000L,
            "<p>Build Spring services</p>",
            "Build Spring services",
            List.of(new LeverList("Requirements", "<ul><li>Java</li></ul>")),
            "<p>Apply to join us</p>",
            "Apply to join us",
            "https://jobs.lever.co/example/" + id,
            "https://jobs.lever.co/example/" + id + "/apply",
            "remote",
            new SalaryRange(new BigDecimal("120000"), new BigDecimal("150000"), "USD", "per-year-salary"),
            null
        );
    }
}
