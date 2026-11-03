package com.kazi.kazi.ingestion.smartrecruiters;

import com.kazi.kazi.ingestion.source.RawJob;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersPostingResponse.Section;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersPostingsResponse.Location;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersPostingsResponse.Posting;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class SmartRecruitersJobSource {

    private final SmartRecruitersClient client;
    private final SmartRecruitersProperties properties;

    SmartRecruitersJobSource(SmartRecruitersClient client, SmartRecruitersProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public List<RawJob> fetchSince(SmartRecruitersProperties.Company company, Instant lastRun) {
        List<RawJob> jobs = new ArrayList<>();
        int offset = 0;
        int expectedTotal = -1;

        while (true) {
            var response = client.getPostings(company.identifier(), properties.pageSize(), offset);
            if (response == null || response.content() == null || response.content().isEmpty()) {
                return jobs;
            }
            if (offset == 0) {
                expectedTotal = response.totalFound() == null ? 0 : response.totalFound();
            }

            response.content().stream()
                .map(posting -> toRawJob(company, posting))
                .forEach(jobs::add);
            offset += response.content().size();
            if ((expectedTotal >= 0 && offset >= expectedTotal) || response.content().size() < properties.pageSize()) {
                return jobs;
            }
        }
    }

    static String sourceName(SmartRecruitersProperties.Company company) {
        return "smartrecruiters:" + company.identifier();
    }

    private RawJob toRawJob(SmartRecruitersProperties.Company company, Posting posting) {
        var detail = client.getPosting(company.identifier(), posting.id());
        Map<String, Object> rawListing = new LinkedHashMap<>();
        rawListing.put("id", posting.id());
        rawListing.put("name", posting.name());
        rawListing.put("location", posting.location());
        rawListing.put("typeOfEmployment", posting.typeOfEmployment());
        rawListing.put("releasedDate", posting.releasedDate() == null ? null : posting.releasedDate().toString());
        Map<String, Object> rawDetail = new LinkedHashMap<>();
        rawDetail.put("id", detail.id());
        rawDetail.put("name", detail.name());
        rawDetail.put("location", detail.location());
        rawDetail.put("typeOfEmployment", detail.typeOfEmployment());
        rawDetail.put("releasedDate", detail.releasedDate() == null ? null : detail.releasedDate().toString());
        rawDetail.put("postingUrl", detail.postingUrl());
        rawDetail.put("applyUrl", detail.applyUrl());
        rawDetail.put("jobAd", detail.jobAd());
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("listing", rawListing);
        rawData.put("detail", rawDetail);

        return new RawJob(
            sourceName(company),
            posting.id(),
            hasText(detail.applyUrl()) ? detail.applyUrl() : detail.postingUrl(),
            hasText(detail.name()) ? detail.name() : posting.name(),
            company.company(),
            location(detail.location() == null ? posting.location() : detail.location()),
            null,
            employmentType(detail, posting),
            description(detail),
            detail.releasedDate() == null ? posting.releasedDate() : detail.releasedDate(),
            Map.of("smartrecruiters", rawData)
        );
    }

    private String employmentType(
        com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersPostingResponse detail,
        Posting posting
    ) {
        var employmentType = detail.typeOfEmployment() == null
            ? posting.typeOfEmployment()
            : detail.typeOfEmployment();
        return employmentType == null ? null : employmentType.label();
    }

    private String location(Location location) {
        if (location == null) {
            return null;
        }
        String value = hasText(location.fullLocation())
            ? location.fullLocation()
            : Stream.of(location.city(), location.region(), location.country())
                .filter(Objects::nonNull)
                .filter(this::hasText)
                .collect(Collectors.joining(", "));
        if (!Boolean.TRUE.equals(location.remote())) {
            return value;
        }
        if (value.isBlank()) {
            return "Remote";
        }
        return value.toLowerCase(Locale.ROOT).contains("remote") ? value : value + ", Remote";
    }

    private String description(com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersPostingResponse detail) {
        if (detail.jobAd() == null || detail.jobAd().sections() == null) {
            return null;
        }
        var sections = detail.jobAd().sections();
        return Stream.of(sections.jobDescription(), sections.qualifications(), sections.additionalInformation())
            .filter(Objects::nonNull)
            .map(Section::text)
            .filter(Objects::nonNull)
            .filter(text -> !text.isBlank())
            .collect(Collectors.joining("\n"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
