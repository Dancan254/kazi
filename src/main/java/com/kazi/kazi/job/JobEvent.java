package com.kazi.kazi.job;

import java.time.Instant;
import java.util.UUID;

public record JobEvent(int schemaVersion, UUID jobId, UUID sourceRunId, Instant occurredAt) {
}
