package com.eventflow.notification.infrastructure;

import com.eventflow.notification.domain.events.NotificationCreatedEvent;
import com.eventflow.notification.domain.events.DispatchResultEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Kafka consumer configuration for EventFlow.
 * Configures consumer factories, error handlers, and listener containers.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(12);
        factory.setCommonErrorHandler(kafkaErrorHandler(kafkaTemplate));

        // Propagate MDC context from Kafka headers
        factory.setRecordInterceptor((record, consumer) -> {
            if (record.headers() != null) {
                var traceIdHeader = record.headers().lastHeader("traceparent");
                if (traceIdHeader != null) {
                    MDC.put("traceId", new String(traceIdHeader.value()));
                }
                var eventIdHeader = record.headers().lastHeader("eventId");
                if (eventIdHeader != null) {
                    MDC.put("eventId", new String(eventIdHeader.value()));
                }
            }
            return record;
        });

        return factory;
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, ex) -> new org.apache.kafka.common.TopicPartition(
                "notification.dlq", record.partition()
            ));

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("Kafka retry attempt {} for record key={}, topic={}: {}",
                deliveryAttempt, record.key(), record.topic(), ex.getMessage());
        });

        return errorHandler;
    }
}