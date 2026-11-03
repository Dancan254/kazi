package com.kazi.kazi.ingestion.personio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.run.SourceRunStatus;
import com.kazi.kazi.job.Job;
import com.kazi.kazi.job.JobRepository;
import com.kazi.kazi.job.JobSourceLink;
import com.kazi.kazi.job.JobSourceLinkRepository;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
    "ingestion.scheduling-enabled=true",
    "ingestion.initial-delay=PT1H",
    "personio.base-url=http://{site}.test/xml",
    "personio.companies[0].company=Acme",
    "personio.companies[0].site=acme",
    "personio.companies[1].company=Beta",
    "personio.companies[1].site=beta",
    "personio.retry.max-retries=0"
})
class PersonioIngestionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PersonioIngestionScheduler scheduler;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private JobRepository jobRepository;

    @MockitoBean
    private PersonioHttpClient httpClient;

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
            .containsExactlyInAnyOrder("personio:acme", "personio:beta");
        assertThat(sourceLinkRepository.findAll())
            .extracting(JobSourceLink::getSourceName, JobSourceLink::getSourceJobId)
            .containsExactlyInAnyOrder(
                org.assertj.core.groups.Tuple.tuple("personio:acme", "job1"),
                org.assertj.core.groups.Tuple.tuple("personio:beta", "job2")
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
        when(httpClient.getPositions(URI.create("http://" + site + ".test/xml"))).thenReturn("""
            <workzag-jobs><position>
              <id>%s</id><office>Nairobi</office><name>%s</name><schedule>full-time</schedule>
              <jobDescriptions><jobDescription><name>Role</name><value>Build Spring services</value></jobDescription></jobDescriptions>
              <createdAt>2026-11-02T10:00:00+00:00</createdAt>
            </position></workzag-jobs>
            """.formatted(id, title));
    }

    private void stubEmptyCompany(String site) {
        when(httpClient.getPositions(URI.create("http://" + site + ".test/xml"))).thenReturn("<workzag-jobs/>");
    }
}
