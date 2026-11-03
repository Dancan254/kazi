package com.kazi.kazi.ingestion.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ingestion.scheduling-enabled", havingValue = "true")
class DiscoveryIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryIngestionScheduler.class);

    private final CareerSiteDiscoveryService discoveryService;
    private final DiscoveryProperties properties;

    DiscoveryIngestionScheduler(CareerSiteDiscoveryService discoveryService, DiscoveryProperties properties) {
        this.discoveryService = discoveryService;
        this.properties = properties;
    }

    @Scheduled(
        fixedDelayString = "${discovery.poll-interval:PT6H}",
        initialDelayString = "${ingestion.initial-delay:PT0S}"
    )
    void ingest() {
        properties.sites().forEach(site -> {
            try {
                discoveryService.ingest(site);
            } catch (Exception exception) {
                log.warn("Career-site discovery failed for {}: {}", site.url(), exception.getMessage());
            }
        });
    }
}
