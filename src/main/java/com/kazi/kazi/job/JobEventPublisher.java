package com.kazi.kazi.job;

import com.kazi.kazi.job.JobService.SavedJob;
import java.time.Clock;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobEventPublisher {

    public static final String INDEXED_TOPIC = "job.events.indexed";
    public static final String MERGED_TOPIC = "job.events.merged";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Clock clock;

    JobEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, Clock clock) {
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;
    }

    public void publish(SavedJob savedJob, UUID sourceRunId) {
        String topic = savedJob.inserted() ? INDEXED_TOPIC : MERGED_TOPIC;
        String jobId = savedJob.id().toString();
        var event = new JobEvent(1, savedJob.id(), sourceRunId, clock.instant());
        kafkaTemplate.send(topic, jobId, event).join();
    }
}
