package com.kazi.kazi.ingestion.workday;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("workday")
public record WorkdayProperties(
    List<Site> sites
) {
    public WorkdayProperties {
        sites = sites == null ? List.of() : sites.stream()
            .filter(site -> hasText(site.company()))
            .filter(site -> hasText(site.baseUrl()))
            .filter(site -> hasText(site.tenant()))
            .filter(site -> hasText(site.site()))
            .map(site -> new Site(
                site.company(),
                site.baseUrl().replaceAll("/+$", ""),
                site.tenant(),
                site.site()
            ))
            .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Site(String company, String baseUrl, String tenant, String site) {
    }
}
