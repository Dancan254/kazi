package com.kazi.kazi.ingestion.personio;

import java.util.List;
import java.util.Map;

record PersonioPosition(
    String id,
    String name,
    String office,
    List<String> additionalOffices,
    String employmentType,
    String schedule,
    String createdAt,
    Map<String, String> descriptions
) {
}
