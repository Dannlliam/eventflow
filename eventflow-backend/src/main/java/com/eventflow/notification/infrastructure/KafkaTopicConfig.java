package com.eventflow.notification.infrastructure;

import org.apache.kafka.clients.admin.NewTopic;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * Kafka topic configuration for EventFlow.
 * Defines all topics used in the event-driven architecture.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        return new KafkaAdmin(Map.of(
            "bootstrap.servers", bootstrapServers
        ));
    }

    @Bean
    public NewTopic notificationCreatedTopic() {
        return TopicBuilder.name("notification.created")
            .partitions(12)
            .replicas(3)
            .config("min.insync.replicas", "2")
            .config("retention.ms", "604800000") // 7 days
            .build();
    }

    @Bean
    public NewTopic dispatchRequestedTopic() {
        return TopicBuilder.name("dispatch.requested")
            .partitions(12)
            .replicas(3)
            .config("min.insync.replicas", "2")
            .build();
    }

    @Bean
    public NewTopic dispatchResultTopic() {
        return TopicBuilder.name("dispatch.result")
            .partitions(12)
            .replicas(3)
            .config("min.insync.replicas", "2")
            .build();
    }

    @Bean
    public NewTopic notificationRetryTopic() {
        return TopicBuilder.name("notification.retry")
            .partitions(6)
            .replicas(3)
            .config("min.insync.replicas", "2")
            .build();
    }

    @Bean
    public NewTopic notificationDlqTopic() {
        return TopicBuilder.name("notification.dlq")
            .partitions(3)
            .replicas(3)
            .config("min.insync.replicas", "2")
            .config("retention.ms", "-1") // Infinite retention
            .build();
    }

    @Bean
    public NewTopic auditEventTopic() {
        return TopicBuilder.name("audit.event")
            .partitions(3)
            .replicas(3)
            .build();
    }

    @Bean
    public NewTopic notificationStatusUpdatedTopic() {
        return TopicBuilder.name("notification.status.updated")
            .partitions(6)
            .replicas(3)
            .build();
    }
}