package com.kazi.kazi.ingestion.recruitee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.recruitee.RecruiteeResponse.Offer;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "recruitee.base-url=http://{site}.test/api/offers/",
    "recruitee.companies[0].company=Acme",
    "recruitee.companies[0].site=acme",
    "recruitee.companies[1].company=Beta",
    "recruitee.companies[1].site=beta",
    "recruitee.retry.max-retries=0"
})
class RecruiteeIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RecruiteeIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private RecruiteeHttpClient httpClient;

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
            .containsExactlyInAnyOrder("recruitee:acme", "recruitee:beta");
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("recruitee:acme", "job1"),
                org.assertj.core.groups.Tuple.tuple("recruitee:beta", "job2")
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
        when(httpClient.getOffers(URI.create("http://" + site + ".test/api/offers/"))).thenReturn(
            new RecruiteeResponse(List.of(new Offer(
                1L,
                id,
                title,
                "Nairobi, Kenya",
                "Nairobi",
                false,
                false,
                "fulltime",
                "2026-11-02 10:09:59 UTC",
                "https://" + site + ".recruitee.com/o/" + id,
                "<p>Build Spring services</p>",
                null,
                null,
                null
            )))
        );
    }

    private void stubEmptyCompany(String site) {
        when(httpClient.getOffers(URI.create("http://" + site + ".test/api/offers/"))).thenReturn(
            new RecruiteeResponse(List.of())
        );
    }
}
