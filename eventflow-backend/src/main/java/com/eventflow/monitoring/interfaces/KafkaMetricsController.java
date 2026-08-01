package com.eventflow.monitoring.interfaces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for exposing Kafka metrics.
 * Provides topic information, consumer lag, and throughput metrics
 * for the Admin Dashboard monitoring UI.
 */
@RestController
@RequestMapping("/api/v1/kafka/metrics")
public class KafkaMetricsController {

    private static final Logger log = LoggerFactory.getLogger(KafkaMetricsController.class);

    /**
     * Get Kafka topic metrics including consumer lag and throughput.
     * 
     * TODO: Integrate with actual Kafka admin client to fetch real metrics
     * - Use AdminClient to list topics and partitions
     * - Query consumer groups for lag information
     * - Calculate messages per second from metrics
     * 
     * @return List of topic metrics
     */
    @GetMapping("/topics")
    public List<TopicMetricsResponse> getTopicMetrics() {
        log.info("Fetching Kafka topic metrics");
        
        // TODO: Replace with real Kafka metrics
        // For now, return empty list so frontend shows "no topics" state
        return List.of();
    }

    /**
     * Get historical consumer lag data for charting.
     * 
     * @return Historical lag data points
     */
    @GetMapping("/lag-history")
    public List<LagHistoryPoint> getLagHistory() {
        log.info("Fetching Kafka lag history");
        
        // TODO: Implement time-series storage for lag metrics
        // Could use Prometheus or custom time-series DB
        return List.of();
    }

    public record TopicMetricsResponse(
        String name,
        int partitions,
        long consumerLag,
        double messagesPerSec
    ) {}

    public record LagHistoryPoint(
        String timestamp,
        Map<String, Long> topicLags
    ) {}
}
