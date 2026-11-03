package com.kazi.kazi.ingestion.workday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.ingestion.workday.WorkdayJobResponse.JobPostingInfo;
import com.kazi.kazi.ingestion.workday.WorkdayJobsResponse.JobPosting;
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
    "workday.sites[0].company=Acme",
    "workday.sites[0].base-url=http://acme.test",
    "workday.sites[0].tenant=acme",
    "workday.sites[0].site=External",
    "workday.sites[1].company=Beta",
    "workday.sites[1].base-url=http://beta.test",
    "workday.sites[1].tenant=beta",
    "workday.sites[1].site=Careers",
    "workday.retry.max-retries=0"
})
class WorkdayIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WorkdayIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private WorkdayHttpClient httpClient;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_ingest_every_configured_site_with_distinct_provenance() {
        stubSite("http://acme.test", "acme", "External", "JR-1", "Java Engineer");
        stubSite("http://beta.test", "beta", "Careers", "JR-2", "Platform Engineer");

        scheduler.ingest();

        assertThat(sourceRunRepository.findAll())
            .allSatisfy(run -> assertThat(run.getStatus()).as(run.getErrorMessage()).isEqualTo(SourceRunStatus.COMPLETED));
        assertThat(sourceRunRepository.findAll())
            .extracting(SourceRun::getSourceName, SourceRun::getStatus)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("workday:acme/External", SourceRunStatus.COMPLETED),
                org.assertj.core.groups.Tuple.tuple("workday:beta/Careers", SourceRunStatus.COMPLETED)
            );
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("workday:acme/External", "/job/JR-1"),
                org.assertj.core.groups.Tuple.tuple("workday:beta/Careers", "/job/JR-2")
            );
        assertThat(jobRepository.findAll())
            .extracting(Job::getCompany)
            .containsExactlyInAnyOrder("acme", "beta");
    }

    @Test
    void should_deactivate_a_job_removed_from_the_workday_site() {
        stubSite("http://acme.test", "acme", "External", "JR-1", "Java Engineer");
        stubEmptySite("http://beta.test", "beta", "Careers");
        scheduler.ingest();
        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("active");

        stubEmptySite("http://acme.test", "acme", "External");
        scheduler.ingest();

        assertThat(jobRepository.findAll().getFirst().getStatus()).isEqualTo("inactive");
        assertThat(sourceLinkRepository.findAll().getFirst().isActive()).isFalse();
    }

    private void stubSite(String baseUrl, String tenant, String site, String id, String title) {
        String path = "/job/" + id;
        when(httpClient.getJobs(eq(jobsUri(baseUrl, tenant, site)), any())).thenReturn(
            new WorkdayJobsResponse(1, List.of(new JobPosting(
                title,
                path,
                "Nairobi, Kenya",
                "Posted Today",
                null,
                List.of(id)
            ))),
            new WorkdayJobsResponse(0, List.of())
        );
        when(httpClient.getJob(URI.create(baseUrl + "/wday/cxs/" + tenant + "/" + site + path))).thenReturn(
            new WorkdayJobResponse(new JobPostingInfo(
                id,
                title,
                "Nairobi, Kenya",
                LocalDate.parse("2026-11-02"),
                "Full Time",
                "<p>Build Spring services</p>",
                "https://careers.example/jobs/" + id,
                null,
                null
            ))
        );
    }

    private void stubEmptySite(String baseUrl, String tenant, String site) {
        when(httpClient.getJobs(eq(jobsUri(baseUrl, tenant, site)), any())).thenReturn(
            new WorkdayJobsResponse(0, List.of())
        );
    }

    private URI jobsUri(String baseUrl, String tenant, String site) {
        return URI.create(baseUrl + "/wday/cxs/" + tenant + "/" + site + "/jobs");
    }
}
