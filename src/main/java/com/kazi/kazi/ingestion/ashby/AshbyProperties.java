package com.kazi.kazi.ingestion.ashby;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ashby")
public record AshbyProperties(
    String baseUrl,
    List<Organization> organizations
) {
    public AshbyProperties {
        organizations = organizations == null ? List.of() : organizations.stream()
            .filter(organization -> organization.company() != null && !organization.company().isBlank())
            .filter(organization -> organization.board() != null && !organization.board().isBlank())
            .toList();
    }

    public record Organization(String company, String board) {
    }
}
