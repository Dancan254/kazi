package com.kazi.kazi.ingestion.recruitee;

import com.kazi.kazi.ingestion.source.JobSourceException;
import com.kazi.kazi.ingestion.source.RawJob;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Component
public class RecruiteeJobSource {

    private static final DateTimeFormatter PUBLISHED_AT_FORMAT = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH)
        .withZone(ZoneOffset.UTC);

    private final RecruiteeHttpClient httpClient;
    private final RecruiteeProperties properties;

    RecruiteeJobSource(RecruiteeHttpClient httpClient, RecruiteeProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @CircuitBreaker(name = "recruitee", fallbackMethod = "unavailable")
    @Retryable(
        maxRetriesString = "${recruitee.retry.max-retries:2}",
        delayString = "${recruitee.retry.delay-ms:2000}",
        jitterString = "${recruitee.retry.jitter-ms:500}",
        multiplierString = "${recruitee.retry.multiplier:2}",
        maxDelayString = "${recruitee.retry.max-delay-ms:30000}"
    )
    public List<RawJob> fetchSince(RecruiteeProperties.Company company, Instant lastRun) {
        RecruiteeResponse response = httpClient.getOffers(endpoint(company));
        if (response == null || response.offers() == null) {
            return List.of();
        }
        return response.offers().stream()
            .map(offer -> toRawJob(company, offer))
            .toList();
    }

    static String sourceName(RecruiteeProperties.Company company) {
        return "recruitee:" + company.site();
    }

    private URI endpoint(RecruiteeProperties.Company company) {
        return URI.create(properties.baseUrl().replace("{site}", company.site()));
    }

    private RawJob toRawJob(RecruiteeProperties.Company company, RecruiteeResponse.Offer offer) {
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("position", offer.position());
        rawData.put("guid", offer.guid());
        rawData.put("title", offer.title());
        rawData.put("location", offer.location());
        rawData.put("locations", offer.locations());
        rawData.put("remote", offer.remote());
        rawData.put("hybrid", offer.hybrid());
        rawData.put("employmentTypeCode", offer.employmentTypeCode());
        rawData.put("publishedAt", offer.publishedAt());
        rawData.put("careersUrl", offer.careersUrl());
        rawData.put("description", offer.description());
        rawData.put("requirements", offer.requirements());
        rawData.put("salary", offer.salary());

        return new RawJob(
            sourceName(company),
            hasText(offer.guid()) ? offer.guid() : String.valueOf(offer.position()),
            offer.careersUrl(),
            offer.title(),
            company.company(),
            location(offer),
            salary(offer.salary()),
            employmentType(offer.employmentTypeCode()),
            description(offer),
            postedAt(offer.publishedAt()),
            Map.of("recruitee", rawData)
        );
    }

    private String location(RecruiteeResponse.Offer offer) {
        String value = offer.locations() == null || offer.locations().isEmpty()
            ? offer.location()
            : offer.locations().stream()
                .map(location -> Stream.of(location.city(), location.state(), location.country())
                    .filter(Objects::nonNull)
                    .filter(this::hasText)
                    .collect(Collectors.joining(", ")))
                .filter(location -> !location.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
        if (value == null) {
            value = offer.city();
        }
        if (!Boolean.TRUE.equals(offer.remote())) {
            return value;
        }
        if (value == null || value.isBlank()) {
            return "Remote";
        }
        return value.toLowerCase(Locale.ROOT).contains("remote") ? value : value + ", Remote";
    }

    private String salary(RecruiteeResponse.Salary salary) {
        if (salary == null || salary.min() == null && salary.max() == null) {
            return null;
        }
        String range = salary.min() == null ? String.valueOf(salary.max())
            : salary.max() == null ? String.valueOf(salary.min())
            : salary.min() + " - " + salary.max();
        return Stream.of(salary.currency(), range, salary.period() == null ? null : "per " + salary.period())
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "));
    }

    private String employmentType(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("fulltime")) {
            return "Full-time";
        }
        if (value.startsWith("parttime")) {
            return "Part-time";
        }
        return value;
    }

    private String description(RecruiteeResponse.Offer offer) {
        String description = Stream.of(offer.description(), offer.requirements())
            .filter(Objects::nonNull)
            .filter(text -> !text.isBlank())
            .collect(Collectors.joining("\n"));
        return description.isBlank() ? null : description;
    }

    private Instant postedAt(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.from(PUBLISHED_AT_FORMAT.parse(value));
            } catch (DateTimeParseException invalidDate) {
                return null;
            }
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<RawJob> unavailable(
        RecruiteeProperties.Company company,
        Instant lastRun,
        Exception exception
    ) {
        throw new JobSourceException(
            "Recruitee company " + company.site() + " is unavailable: " + exception,
            exception
        );
    }
}
