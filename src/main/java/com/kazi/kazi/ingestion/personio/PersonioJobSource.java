package com.kazi.kazi.ingestion.personio;

import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

@Component
public class PersonioJobSource {

    private final PersonioHttpClient httpClient;
    private final PersonioProperties properties;

    PersonioJobSource(PersonioHttpClient httpClient, PersonioProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @CircuitBreaker(name = "personio", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${personio.retry.max-retries:2}",
        delayString = "${personio.retry.delay-ms:2000}",
        jitterString = "${personio.retry.jitter-ms:500}",
        multiplierString = "${personio.retry.multiplier:2}",
        maxDelayString = "${personio.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(PersonioProperties.Company company, Instant lastRun) {
        String xml = httpClient.getPositions(URI.create(properties.baseUrl().replace("{site}", company.site())));
        try {
            return PersonioXmlParser.parse(xml).stream()
                .map(position -> toRawJob(company, position))
                .toList();
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new JobSourceException("Personio company " + company.site() + " returned invalid XML", exception);
        }
    }

    static String sourceName(PersonioProperties.Company company) {
        return "personio:" + company.site();
    }

    private RawJob toRawJob(PersonioProperties.Company company, PersonioPosition position) {
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("id", position.id());
        rawData.put("name", position.name());
        rawData.put("office", position.office());
        rawData.put("additionalOffices", position.additionalOffices());
        rawData.put("employmentType", position.employmentType());
        rawData.put("schedule", position.schedule());
        rawData.put("createdAt", position.createdAt());
        rawData.put("jobDescriptions", position.descriptions());

        return new RawJob(
            sourceName(company),
            position.id(),
            properties.baseUrl().replace("{site}", company.site()).replaceAll("/xml/?$", "") + "/job/" + position.id(),
            position.name(),
            company.company(),
            Stream.concat(Stream.of(position.office()), position.additionalOffices().stream())
                .filter(office -> !office.isBlank())
                .collect(Collectors.joining(", ")),
            null,
            position.schedule().isBlank() ? position.employmentType() : position.schedule(),
            String.join("\n", position.descriptions().values()),
            postedAt(position.createdAt()),
            Map.of("personio", rawData)
        );
    }

    private Instant postedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private List<RawJob> unavailable(
        PersonioProperties.Company company,
        Instant lastRun,
        Exception exception
    ) {
        throw new JobSourceException(
            "Personio company " + company.site() + " is unavailable: " + exception,
            exception
        );
    }
}
