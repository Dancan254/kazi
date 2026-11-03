package com.kazi.kazi.ingestion.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kazi.kazi.ingestion.IngestionService;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.JobSourceException;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CareerSiteDiscoveryServiceTest {

    @Mock private DiscoveryHttpClient httpClient;
    @Mock private CareerSiteRouter router;
    @Mock private IngestionService ingestionService;

    private CareerSiteDiscoveryService service;

    @BeforeEach
    void setUp() {
        service = new CareerSiteDiscoveryService(httpClient, router, ingestionService);
    }

    @Test
    void should_use_a_direct_ats_url_without_fetching_html() {
        JobSource source = mock(JobSource.class);
        when(router.route("Acme", URI.create("https://boards.greenhouse.io/acme")))
            .thenReturn(Optional.of(source));

        assertThat(service.discover("Acme", "https://boards.greenhouse.io/acme")).containsExactly(source);
        verify(httpClient, never()).getPage(any());
    }

    @Test
    void should_discover_and_deduplicate_ats_links_from_a_public_career_page() {
        URI page = URI.create("https://93.184.216.34/careers");
        URI board = URI.create("https://boards.greenhouse.io/acme");
        JobSource source = mock(JobSource.class);
        when(source.sourceName()).thenReturn("greenhouse:acme");
        when(router.route("Acme", page)).thenReturn(Optional.empty());
        when(router.route("Acme", board)).thenReturn(Optional.of(source));
        when(httpClient.getPage(page)).thenReturn("""
            <html><a href="https://boards.greenhouse.io/acme">Jobs</a>
            <a href="https://boards.greenhouse.io/acme">Duplicate</a></html>
            """);

        assertThat(service.discover("Acme", page.toString())).containsExactly(source);
    }

    @Test
    void should_reject_private_career_page_addresses() {
        assertThatThrownBy(() -> service.discover("Acme", "http://127.0.0.1/careers"))
            .isInstanceOf(JobSourceException.class)
            .hasMessageContaining("private address");
        verify(httpClient, never()).getPage(any());
    }
}
