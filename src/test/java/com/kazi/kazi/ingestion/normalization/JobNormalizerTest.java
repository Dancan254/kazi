package com.kazi.kazi.ingestion.normalization;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JobNormalizerTest {

    private final JobNormalizer normalizer = new JobNormalizer();

    @Test
    void should_clean_company_and_expand_title_abbreviations() {
        var job = normalizer.normalize(rawJob("Sr. SWE (Backend)", "JPMorgan Chase & Co.", null, null, null));

        assertThat(job.company()).isEqualTo("jpmorgan chase co");
        assertThat(job.title()).isEqualTo("senior software engineer backend");
    }

    @Test
    void should_convert_html_description_to_plain_text() {
        var job = normalizer.normalize(rawJobWithDescription(
            "<div><h3>The Role</h3><p>Build Java &amp; Spring services.<br>Remote &amp; flexible.</p>"
                + "<ul><li>Java</li><li>Kafka</li></ul><script>ignored()</script></div>"
        ));

        assertThat(job.description()).isEqualTo(
            "The Role\nBuild Java & Spring services.\nRemote & flexible.\n- Java\n- Kafka"
        );
        assertThat(job.rawJob().description()).contains("<h3>");
    }

    @Test
    void should_remove_entity_encoded_html_description() {
        var job = normalizer.normalize(rawJobWithDescription(
            "&lt;p&gt;Build Java &amp;amp; Spring services.&lt;/p&gt;"
        ));

        assertThat(job.description()).isEqualTo("Build Java & Spring services.");
    }

    @Test
    void should_parse_location_and_detect_remote_work() {
        var job = normalizer.normalize(rawJob("Developer", "Acme", "Nairobi, Kenya (Remote)", null, null));

        assertThat(job.city()).isEqualTo("nairobi");
        assertThat(job.country()).isEqualTo("kenya");
        assertThat(job.remote()).isTrue();
    }

    @Test
    void should_extract_salary_range_and_currency() {
        var job = normalizer.normalize(rawJob("Developer", "Acme", "Remote", "$120k - $150k", null));

        assertThat(job.salaryMin()).isEqualTo(120_000);
        assertThat(job.salaryMax()).isEqualTo(150_000);
        assertThat(job.currency()).isEqualTo("USD");
    }

    @Test
    void should_annualize_hourly_salary() {
        var job = normalizer.normalize(rawJob("Developer", "Acme", "Remote", "$60 per hour", null));

        assertThat(job.salaryMin()).isEqualTo(124_800);
        assertThat(job.salaryMax()).isEqualTo(124_800);
    }

    @Test
    void should_map_common_employment_types() {
        assertThat(normalizeEmploymentType("Full Time")).isEqualTo(EmploymentType.FULL_TIME);
        assertThat(normalizeEmploymentType("FTE")).isEqualTo(EmploymentType.FULL_TIME);
        assertThat(normalizeEmploymentType("Part Time")).isEqualTo(EmploymentType.PART_TIME);
        assertThat(normalizeEmploymentType("Contract")).isEqualTo(EmploymentType.CONTRACT);
        assertThat(normalizeEmploymentType("Internship")).isEqualTo(EmploymentType.INTERNSHIP);
    }

    private EmploymentType normalizeEmploymentType(String employmentType) {
        return normalizer.normalize(rawJob("Developer", "Acme", "Remote", null, employmentType)).employmentType();
    }

    private RawJob rawJob(String title, String company, String location, String salary, String employmentType) {
        return rawJob(title, company, location, salary, employmentType, "Description");
    }

    private RawJob rawJobWithDescription(String description) {
        return rawJob("Developer", "Acme", "Remote", null, null, description);
    }

    private RawJob rawJob(
        String title,
        String company,
        String location,
        String salary,
        String employmentType,
        String description
    ) {
        return new RawJob(
            "test",
            "job-1",
            "https://example.com/jobs/1",
            title,
            company,
            location,
            salary,
            employmentType,
            description,
            Instant.parse("2026-11-01T00:00:00Z"),
            Map.of("id", "job-1")
        );
    }
}
