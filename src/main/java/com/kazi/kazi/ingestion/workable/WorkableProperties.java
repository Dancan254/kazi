package com.kazi.kazi.ingestion.workable;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("workable")
public record WorkableProperties(
    String baseUrl,
    List<Account> accounts
) {
    public WorkableProperties {
        accounts = accounts == null ? List.of() : accounts.stream()
            .filter(account -> hasText(account.company()))
            .filter(account -> hasText(account.account()))
            .toList();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Account(String company, String account) {
    }
}
