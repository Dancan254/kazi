package com.kazi.kazi.job;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
class JobMessagingConfig {

    @Bean
    NewTopic jobIndexedTopic() {
        return topic(JobEventPublisher.INDEXED_TOPIC);
    }

    @Bean
    NewTopic jobMergedTopic() {
        return topic(JobEventPublisher.MERGED_TOPIC);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}
