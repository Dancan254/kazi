package com.kazi.kazi.ingestion.personio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kazi.kazi.ingestion.personio.PersonioProperties.Company;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class PersonioJobSourceTest {

    private final PersonioJobSource jobSource;
    private final MockRestServiceServer server;

    PersonioJobSourceTest() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        PersonioHttpClient httpClient = HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(restClient))
            .build()
            .createClient(PersonioHttpClient.class);
        jobSource = new PersonioJobSource(
            httpClient,
            new PersonioProperties("http://localhost/{site}/xml", null)
        );
    }

    @Test
    void should_map_personio_xml_positions() {
        server.expect(requestTo(containsString("/acme/xml")))
            .andRespond(withSuccess("""
                <?xml version="1.0" encoding="UTF-8"?>
                <workzag-jobs>
                  <position>
                    <id>1834171</id>
                    <office>Berlin/Remote</office>
                    <additionalOffices><office>Nairobi</office></additionalOffices>
                    <name>Staff Software Engineer</name>
                    <jobDescriptions>
                      <jobDescription><name>About the Role</name><value>&lt;p&gt;Build Spring services&lt;/p&gt;</value></jobDescription>
                    </jobDescriptions>
                    <employmentType>permanent</employmentType>
                    <schedule>full-time</schedule>
                    <createdAt>2026-11-02T14:10:41+00:00</createdAt>
                  </position>
                </workzag-jobs>
                """, MediaType.APPLICATION_XML));

        var jobs = jobSource.fetchSince(new Company("Acme", "acme"), null);

        assertThat(jobs).hasSize(1);
        var job = jobs.getFirst();
        assertThat(job.sourceName()).isEqualTo("personio:acme");
        assertThat(job.sourceJobId()).isEqualTo("1834171");
        assertThat(job.sourceUrl()).isEqualTo("http://localhost/acme/job/1834171");
        assertThat(job.company()).isEqualTo("Acme");
        assertThat(job.location()).isEqualTo("Berlin/Remote, Nairobi");
        assertThat(job.employmentType()).isEqualTo("full-time");
        assertThat(job.description()).isEqualTo("<p>Build Spring services</p>");
        assertThat(job.postedAt()).isEqualTo("2026-11-02T14:10:41Z");
        assertThat(job.rawData()).containsKey("personio");
        server.verify();
    }
}
