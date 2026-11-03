package com.kazi.kazi.search;

import com.kazi.kazi.job.JobEvent;
import com.kazi.kazi.job.JobEventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "kazi.semantic.enabled", havingValue = "true")
class JobEmbeddingConsumer {

    private final SemanticSearchService semanticSearchService;

    JobEmbeddingConsumer(SemanticSearchService semanticSearchService) {
        this.semanticSearchService = semanticSearchService;
    }

    @KafkaListener(
        topics = {JobEventPublisher.INDEXED_TOPIC, JobEventPublisher.MERGED_TOPIC},
        groupId = "kazi-semantic"
    )
    void onJobChanged(JobEvent event) {
        semanticSearchService.index(event.jobId());
    }
}
