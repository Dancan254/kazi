package com.kazi.kazi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration(proxyBeanMethods = false)
@Import(IntegrationTestContainers.class)
class TestcontainersConfiguration {
}
