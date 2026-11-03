package com.kazi.kazi.ingestion.recruitee;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("recruitee")
public record RecruiteeProperties(
    String baseUrl,
    List<Company> companies
) {
    public RecruiteeProperties {
        companies = companies == null ? List.of() : companies.stream()
            .filter(company -> hasText(company.company()))
            .filter(company -> hasText(company.site()))
            .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Company(String company, String site) {
    }
}
