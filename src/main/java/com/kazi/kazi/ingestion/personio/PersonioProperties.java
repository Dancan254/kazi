package com.kazi.kazi.ingestion.personio;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("personio")
public record PersonioProperties(
    String baseUrl,
    List<Company> companies
) {
    public PersonioProperties {
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
