package com.kazi.kazi.ingestion.workday;

import com.kazi.kazi.ingestion.source.RawJob;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
public class WorkdayJobSource {

    private static final int PAGE_SIZE = 20;

    private final WorkdayClient client;

    WorkdayJobSource(WorkdayClient client) {
        this.client = client;
    }

    public List<RawJob> fetchSince(WorkdayProperties.Site site, Instant lastRun) {
        List<RawJob> jobs = new ArrayList<>();
        int offset = 0;
        int expectedTotal = 0;

        while (true) {
            var response = client.getJobs(
                endpoint(site, "/jobs"),
                new WorkdaySearchRequest(Map.of(), PAGE_SIZE, offset, "")
            );
            if (response == null || response.jobPostings() == null || response.jobPostings().isEmpty()) {
                return jobs;
            }
            if (offset == 0) {
                expectedTotal = response.total();
            }

            response.jobPostings().stream()
                .map(posting -> toRawJob(site, posting))
                .forEach(jobs::add);
            offset += PAGE_SIZE;
            if (expectedTotal > 0) {
                if (offset >= expectedTotal) {
                    return jobs;
                }
            } else if (response.jobPostings().size() < PAGE_SIZE) {
                return jobs;
            }
        }
    }

    static String sourceName(WorkdayProperties.Site site) {
        return "workday:" + site.tenant() + "/" + site.site();
    }

    private RawJob toRawJob(WorkdayProperties.Site site, WorkdayJobsResponse.JobPosting posting) {
        WorkdayJobResponse.JobPostingInfo detail = client
            .getJob(endpoint(site, posting.externalPath()))
            .jobPostingInfo();
        Map<String, Object> rawDetail = new LinkedHashMap<>();
        rawDetail.put("jobReqId", detail.jobReqId());
        rawDetail.put("title", detail.title());
        rawDetail.put("location", detail.location());
        rawDetail.put("startDate", detail.startDate() == null ? null : detail.startDate().toString());
        rawDetail.put("timeType", detail.timeType());
        rawDetail.put("jobDescription", detail.jobDescription());
        rawDetail.put("externalUrl", detail.externalUrl());
        rawDetail.put("jobRequisitionLocation", detail.jobRequisitionLocation());
        rawDetail.put("additionalLocations", detail.additionalLocations());
        Map<String, Object> rawData = new LinkedHashMap<>();
        rawData.put("listing", posting);
        rawData.put("detail", rawDetail);

        return new RawJob(
            sourceName(site),
            hasText(posting.externalPath()) ? posting.externalPath() : detail.jobReqId(),
            hasText(detail.externalUrl()) ? detail.externalUrl() : site.baseUrl() + posting.externalPath(),
            hasText(detail.title()) ? detail.title() : posting.title(),
            site.company(),
            location(posting, detail),
            null,
            detail.timeType(),
            detail.jobDescription(),
            detail.startDate() == null ? null : detail.startDate().atStartOfDay().toInstant(ZoneOffset.UTC),
            Map.of("workday", rawData)
        );
    }

    private String location(
        WorkdayJobsResponse.JobPosting posting,
        WorkdayJobResponse.JobPostingInfo detail
    ) {
        String locations = Stream.concat(
                Stream.of(detail.location()),
                detail.additionalLocations() == null
                    ? Stream.empty()
                    : detail.additionalLocations().stream()
            )
            .filter(Objects::nonNull)
            .filter(location -> !location.isBlank())
            .collect(Collectors.joining(", "));
        if (locations.isBlank()) {
            locations = posting.locationsText();
        }
        if (!"Remote".equalsIgnoreCase(posting.remoteType())) {
            return locations;
        }
        if (locations == null || locations.isBlank()) {
            return "Remote";
        }
        return locations.toLowerCase(java.util.Locale.ROOT).contains("remote")
            ? locations
            : locations + ", Remote";
    }

    private URI endpoint(WorkdayProperties.Site site, String path) {
        return URI.create(site.baseUrl() + "/wday/cxs/" + site.tenant() + "/" + site.site() + path);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}
