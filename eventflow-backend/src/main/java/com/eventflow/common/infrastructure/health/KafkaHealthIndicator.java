package com.eventflow.common.infrastructure.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Health indicator for Kafka cluster connectivity.
 * Checks if the Kafka cluster is reachable and the broker is available.
 * Used by the readiness probe in the Kubernetes deployment.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try {
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                DescribeClusterResult clusterResult = adminClient.describeCluster();

                String clusterId = clusterResult.clusterId().get(5, TimeUnit.SECONDS);
                int nodeCount = clusterResult.nodes().get(5, TimeUnit.SECONDS).size();

                return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("brokerCount", nodeCount)
                    .withDetail("status", "connected")
                    .build();
            }
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return Health.down()
                .withDetail("status", "disconnected")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}