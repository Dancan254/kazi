package com.kazi.kazi.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

class JobEventSerializationTest {

    @Test
    void should_serialize_event_as_json() {
        var event = new JobEvent(1, UUID.randomUUID(), UUID.randomUUID(), Instant.now());

        byte[] json;
        try (var serializer = new JacksonJsonSerializer<JobEvent>()) {
            json = serializer.serialize(JobEventPublisher.INDEXED_TOPIC, event);
        }

        assertThat(new String(json, StandardCharsets.UTF_8))
            .contains("\"schemaVersion\":1")
            .contains("\"occurredAt\":");
    }
}
