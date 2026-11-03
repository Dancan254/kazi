package com.kazi.kazi.ingestion.discovery;

import com.kazi.kazi.ingestion.IngestionService;
import com.kazi.kazi.ingestion.run.SourceRun;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.JobSourceException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
public class CareerSiteDiscoveryService {

    private static final Pattern HREF = Pattern.compile("href\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final int MAX_PAGE_CHARS = 2_000_000;
    private static final int MAX_SOURCES = 25;

    private final DiscoveryHttpClient httpClient;
    private final CareerSiteRouter router;
    private final IngestionService ingestionService;

    CareerSiteDiscoveryService(
        DiscoveryHttpClient httpClient,
        CareerSiteRouter router,
        IngestionService ingestionService
    ) {
        this.httpClient = httpClient;
        this.router = router;
        this.ingestionService = ingestionService;
    }

    public List<JobSource> discover(String company, String url) {
        URI page = URI.create(url);
        var direct = router.route(company, page);
        if (direct.isPresent()) {
            return List.of(direct.get());
        }
        validatePublic(page);
        String html;
        try {
            html = httpClient.getPage(page);
        } catch (RestClientException exception) {
            throw new JobSourceException("Unable to read career page " + page, exception);
        }
        if (html == null || html.isBlank()) {
            return List.of();
        }
        String content = html.length() > MAX_PAGE_CHARS ? html.substring(0, MAX_PAGE_CHARS) : html;
        Map<String, JobSource> sources = new LinkedHashMap<>();
        var matcher = HREF.matcher(content);
        while (matcher.find() && sources.size() < MAX_SOURCES) {
            try {
                URI candidate = page.resolve(matcher.group(1).replace("&amp;", "&"));
                router.route(company, candidate).ifPresent(source -> sources.putIfAbsent(source.sourceName(), source));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed links on an otherwise usable career page.
            }
        }
        return List.copyOf(sources.values());
    }

    public List<SourceRun> ingest(DiscoveryProperties.Site site) {
        return ingestionService.ingestAll(discover(site.company(), site.url()));
    }

    private void validatePublic(URI uri) {
        if (uri.getHost() == null || !"https".equalsIgnoreCase(uri.getScheme())
            && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new JobSourceException("Career page must use HTTP or HTTPS: " + uri, null);
        }
        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (!isPublic(address)) {
                    throw new JobSourceException("Career page resolves to a private address: " + uri, null);
                }
            }
        } catch (UnknownHostException exception) {
            throw new JobSourceException("Career page host cannot be resolved: " + uri, exception);
        }
    }

    private boolean isPublic(InetAddress address) {
        byte[] bytes = address.getAddress();
        boolean uniqueLocal = bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        return !address.isAnyLocalAddress()
            && !address.isLoopbackAddress()
            && !address.isLinkLocalAddress()
            && !address.isSiteLocalAddress()
            && !address.isMulticastAddress()
            && !uniqueLocal;
    }
}
