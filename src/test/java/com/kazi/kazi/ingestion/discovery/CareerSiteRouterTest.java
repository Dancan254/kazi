package com.kazi.kazi.ingestion.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import com.kazi.kazi.ingestion.ashby.AshbyJobSource;
import com.kazi.kazi.ingestion.bamboohr.BambooHrJobSource;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseJobSource;
import com.kazi.kazi.ingestion.lever.LeverJobSource;
import com.kazi.kazi.ingestion.personio.PersonioJobSource;
import com.kazi.kazi.ingestion.recruitee.RecruiteeJobSource;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersJobSource;
import com.kazi.kazi.ingestion.workable.WorkableJobSource;
import com.kazi.kazi.ingestion.workday.WorkdayJobSource;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareerSiteRouterTest {

    @Mock private GreenhouseJobSource greenhouse;
    @Mock private LeverJobSource lever;
    @Mock private AshbyJobSource ashby;
    @Mock private WorkdayJobSource workday;
    @Mock private SmartRecruitersJobSource smartRecruiters;
    @Mock private WorkableJobSource workable;
    @Mock private RecruiteeJobSource recruitee;
    @Mock private PersonioJobSource personio;
    @Mock private BambooHrJobSource bambooHr;

    private CareerSiteRouter router;

    @BeforeEach
    void setUp() {
        router = new CareerSiteRouter(
            greenhouse, lever, ashby, workday, smartRecruiters, workable, recruitee, personio, bambooHr
        );
    }

    @ParameterizedTest
    @CsvSource({
        "https://boards.greenhouse.io/acme, greenhouse:acme",
        "https://job-boards.greenhouse.io/acme, greenhouse:acme",
        "https://jobs.lever.co/acme, lever:acme",
        "https://jobs.ashbyhq.com/acme, ashby:acme",
        "https://acme.wd5.myworkdayjobs.com/en-US/External, workday:acme/External",
        "https://jobs.smartrecruiters.com/Acme, smartrecruiters:Acme",
        "https://apply.workable.com/acme/, workable:acme",
        "https://acme.recruitee.com/, recruitee:acme",
        "https://acme.jobs.personio.de/, personio:acme",
        "https://acme.bamboohr.com/careers, bamboohr:acme"
    })
    void should_route_supported_career_sites(URI uri, String expectedSource) {
        var source = router.route("Acme", uri);

        assertThat(source).isPresent();
        assertThat(source.get().sourceName()).isEqualTo(expectedSource);
        assertThat(source.get().providesFullSnapshot()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://example.com/careers",
        "https://apply.workable.com/j/ABC123",
        "file:///tmp/jobs.html"
    })
    void should_reject_unsupported_career_sites(URI uri) {
        assertThat(router.route("Acme", uri)).isEmpty();
    }
}
