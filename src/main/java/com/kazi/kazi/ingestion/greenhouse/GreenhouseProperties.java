package com.kazi.kazi.ingestion.greenhouse;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("greenhouse")
public record GreenhouseProperties(
    String baseUrl,
    List<Board> boards
) {
    public GreenhouseProperties {
        boards = boards == null ? List.of() : boards.stream()
            .filter(board -> board.company() != null && !board.company().isBlank())
            .filter(board -> board.token() != null && !board.token().isBlank())
            .toList();
    }

    public record Board(String company, String token) {
    }
}
