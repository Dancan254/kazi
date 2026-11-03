package com.kazi.kazi.ingestion.smartrecruiters;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("smartrecruiters")
public record SmartRecruitersProperties(
    String baseUrl,
    Integer pageSize,
    List<Company> companies
) {
    public SmartRecruitersProperties {
        pageSize = pageSize == null || pageSize < 1 || pageSize > 100 ? 100 : pageSize;
        companies = companies == null ? List.of() : companies.stream()
            .filter(company -> hasText(company.company()))
            .filter(company -> hasText(company.identifier()))
            .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Company(String company, String identifier) {
    }
}
