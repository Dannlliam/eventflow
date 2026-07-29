package com.eventflow.common.domain;

/**
 * Base interface for domain events in the EventFlow system.
 * Domain events are immutable records of something that happened in the domain.
 */
public interface DomainEvent {
    String getEventId();
    String getEventType();
    long getTimestamp();
}