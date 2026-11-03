package com.kazi.kazi.ingestion.bamboohr;

import com.kazi.kazi.ingestion.bamboohr.BambooHrDetailResponse.JobOpening;
import com.kazi.kazi.ingestion.bamboohr.BambooHrListResponse.BambooHrJob;
import com.kazi.kazi.ingestion.bamboohr.BambooHrListResponse.Location;
import com.kazi.kazi.ingestion.source.RawJob;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class BambooHrJobSource {

    private final BambooHrClient client;
    private final BambooHrProperties properties;

    BambooHrJobSource(BambooHrClient client, BambooHrProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<RawJob> fetchSince(BambooHrProperties.Company company, Instant lastRun) {
        var response = client.getJobs(endpoint(company, "/list"));
        if (response == null || response.result() == null) {
            return List.of();
        }
        return response.result().stream()
            .map(job -> toRawJob(company, job))
            .toList();
    }

    static String sourceName(BambooHrProperties.Company company) {
        return "bamboohr:" + company.site();
    }

    private RawJob toRawJob(BambooHrProperties.Company company, BambooHrJob job) {
        JobOpening detail = client.getJob(endpoint(company, "/" + job.id() + "/detail")).result().jobOpening();
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("listing", job);
        rawData.put("jobOpeningShareUrl", detail.jobOpeningShareUrl());
        rawData.put("jobOpeningName", detail.jobOpeningName());
        rawData.put("employmentStatusLabel", detail.employmentStatusLabel());
        rawData.put("employmentType", detail.employmentType());
        rawData.put("location", detail.location());
        rawData.put("atsLocation", detail.atsLocation());
        rawData.put("description", detail.description());
        rawData.put("compensation", detail.compensation());
        rawData.put("datePosted", detail.datePosted() == null ? null : detail.datePosted().toString());
        rawData.put("locationType", detail.locationType());

        return new RawJob(
            sourceName(company),
            job.id(),
            hasText(detail.jobOpeningShareUrl())
                ? detail.jobOpeningShareUrl()
                : baseUrl(company) + "/" + job.id(),
            hasText(detail.jobOpeningName()) ? detail.jobOpeningName() : job.jobOpeningName(),
            company.company(),
            location(job, detail),
            detail.compensation(),
            hasText(detail.employmentType()) ? detail.employmentType() : detail.employmentStatusLabel(),
            detail.description(),
            detail.datePosted() == null ? null : detail.datePosted().atStartOfDay().toInstant(ZoneOffset.UTC),
            Map.of("bamboohr", rawData)
        );
    }

    private String location(BambooHrJob job, JobOpening detail) {
        Location atsLocation = detail.atsLocation() == null ? job.atsLocation() : detail.atsLocation();
        Location location = detail.location() == null ? job.location() : detail.location();
        String value = hasLocation(atsLocation)
            ? locationText(atsLocation.city(), atsLocation.state(), atsLocation.country())
            : locationText(location);
        boolean remote = Boolean.TRUE.equals(job.isRemote())
            || "1".equals(detail.locationType() == null ? job.locationType() : detail.locationType());
        if (!remote) {
            return value;
        }
        if (value.isBlank()) {
            return "Remote";
        }
        return value.toLowerCase(Locale.ROOT).contains("remote") ? value : value + ", Remote";
    }

    private String locationText(Location location) {
        if (location == null) {
            return "";
        }
        return locationText(location.city(), location.state(), location.addressCountry());
    }

    private String locationText(String city, String state, String country) {
        return Stream.of(city, state, country)
            .filter(Objects::nonNull)
            .filter(this::hasText)
            .collect(Collectors.joining(", "));
    }

    private boolean hasLocation(Location location) {
        return location != null && !locationText(location.city(), location.state(), location.country()).isBlank();
    }

    private URI endpoint(BambooHrProperties.Company company, String path) {
        return URI.create(baseUrl(company) + path);
    }

    private String baseUrl(BambooHrProperties.Company company) {
        return properties.baseUrl().replace("{site}", company.site()).replaceAll("/+$", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
