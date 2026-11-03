package com.kazi.kazi.ingestion.workday;

import java.util.Map;

record WorkdaySearchRequest(
    Map<String, Object> appliedFacets,
    int limit,
    int offset,
    String searchText
) {
}
