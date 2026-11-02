package com.kazi.kazi.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.kazi.kazi.BaseIntegrationTest;
import com.kazi.kazi.ingestion.IngestionService;
import com.kazi.kazi.ingestion.run.SourceRunRepository;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.source.RawJob;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;

class JobEventPublisherIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobSourceLinkRepository sourceLinkRepository;

    @Autowired
    private SourceRunRepository sourceRunRepository;

    @Autowired
    private TestJobEventListener listener;

    @BeforeEach
    void cleanJobs() {
        sourceLinkRepository.deleteAll();
        jobRepository.deleteAll();
        sourceRunRepository.deleteAll();
    }

    @Test
    void should_publish_indexed_and_merged_events_with_source_run_correlation() {
        var firstRun = ingestionService.ingest(source());
        var secondRun = ingestionService.ingest(source());

        assertThat(firstRun.getErrorMessage()).isNull();
        assertThat(secondRun.getErrorMessage()).isNull();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
            assertThat(listener.events).hasSize(2)
        );
        Map<String, ConsumerRecord<String, JobEvent>> events = listener.events.stream()
            .collect(Collectors.toMap(ConsumerRecord::topic, Function.identity()));
        assertThat(events).containsOnlyKeys(JobEventPublisher.INDEXED_TOPIC, JobEventPublisher.MERGED_TOPIC);

        String jobId = jobRepository.findAll().getFirst().getId().toString();
        var indexed = events.get(JobEventPublisher.INDEXED_TOPIC);
        var merged = events.get(JobEventPublisher.MERGED_TOPIC);
        assertThat(indexed.key()).isEqualTo(jobId);
        assertThat(indexed.value().jobId().toString()).isEqualTo(jobId);
        assertThat(indexed.value().sourceRunId()).isEqualTo(firstRun.getId());
        assertThat(indexed.value().schemaVersion()).isEqualTo(1);
        assertThat(indexed.value().occurredAt()).isNotNull();
        assertThat(merged.value().sourceRunId()).isEqualTo(secondRun.getId());
    }

    private JobSource source() {
        return new JobSource() {
            @Override
            public String sourceName() {
                return "test-source";
            }

            @Override
            public List<RawJob> fetchSince(Instant lastRun) {
                return List.of(new RawJob(
                    sourceName(),
                    "job-1",
                    "https://example.com/job-1",
                    "Java Developer",
                    "Acme",
                    "Remote",
                    null,
                    "Full Time",
                    "Description",
                    Instant.parse("2026-11-01T00:00:00Z"),
                    Map.of("id", "job-1")
                ));
            }
        };
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaTestConfig {

        @Bean
        TestJobEventListener testJobEventListener() {
            return new TestJobEventListener();
        }
    }

    static class TestJobEventListener {

        private final Queue<ConsumerRecord<String, JobEvent>> events = new ConcurrentLinkedQueue<>();

        @KafkaListener(
            topics = {JobEventPublisher.INDEXED_TOPIC, JobEventPublisher.MERGED_TOPIC},
            groupId = "job-event-publisher-test"
        )
        void listen(ConsumerRecord<String, JobEvent> event) {
            events.add(event);
        }
    }
}
