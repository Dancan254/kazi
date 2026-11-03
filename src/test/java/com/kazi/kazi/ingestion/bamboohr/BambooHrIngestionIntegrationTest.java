package com.kazi.kazi.ingestion.bamboohr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.bamboohr.BambooHrDetailResponse.JobOpening;
import com.kazi.kazi.ingestion.bamboohr.BambooHrListResponse.BambooHrJob;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "bamboohr.base-url=http://{site}.test/careers",
    "bamboohr.companies[0].company=Acme",
    "bamboohr.companies[0].site=acme",
    "bamboohr.companies[1].company=Beta",
    "bamboohr.companies[1].site=beta",
    "bamboohr.retry.max-retries=0"
})
class BambooHrIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BambooHrIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private BambooHrClient client;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_company_with_distinct_provenance() {
        stubCompany("acme", "job1", "Java Engineer");
        stubCompany("beta", "job2", "Platform Engineer");

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName)
            .containsExactlyInAnyOrder("bamboohr:acme", "bamboohr:beta");
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("bamboohr:acme", "job1"),
                org.assertj.core.groups.Tuple.tuple("bamboohr:beta", "job2")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    @Test
    void should_deactivate_a_job_removed_from_the_company() {
        stubCompany("acme", "job1", "Java Engineer");
        stubEmptyCompany("beta");
        scheduler.ingest();
        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");

        stubEmptyCompany("acme");
        scheduler.ingest();

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isFalse();
    }

    private void stubCompany(String site, String id, String title) {
        when(client.getJobs(URI.create("http://" + site + ".test/careers/list"))).thenReturn(
            new BambooHrListResponse(null, List.of(new BambooHrJob(
                id, title, "Full-time", null, null, null, false, "0"
            )))
        );
        when(client.getJob(URI.create("http://" + site + ".test/careers/" + id + "/detail"))).thenReturn(
            new BambooHrDetailResponse(new BambooHrDetailResponse.Result(new JobOpening(
                "https://" + site + ".bamboohr.com/careers/" + id,
                title,
                "Full-time",
                null,
                new BambooHrListResponse.Location("Nairobi", null, null, null, "Kenya"),
                null,
                "<p>Build Spring services</p>",
                null,
                LocalDate.parse("2026-11-02"),
                "0"
            )))
        );
    }

    private void stubEmptyCompany(String site) {
        when(client.getJobs(URI.create("http://" + site + ".test/careers/list"))).thenReturn(
            new BambooHrListResponse(null, List.of())
        );
    }
}
