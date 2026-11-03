package com.kazi.kazi.ingestion.lever;

import java.math.BigDecimal;
import java.util.List;

record LeverPosting(
    String id,
    String text,
    Categories categories,
    String country,
    Long createdAt,
    String description,
    String descriptionPlain,
    List<LeverList> lists,
    String additional,
    String additionalPlain,
    String hostedUrl,
    String applyUrl,
    String workplaceType,
    SalaryRange salaryRange,
    String salaryDescriptionPlain
) {
    record Categories(
        String commitment,
        String department,
        String location,
        String team,
        List<String> allLocations
    ) {
    }

    record LeverList(String text, String content) {
    }

    record SalaryRange(BigDecimal min, BigDecimal max, String currency, String interval) {
    }
}
