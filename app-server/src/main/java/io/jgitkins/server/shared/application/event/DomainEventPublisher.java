package io.jgitkins.server.shared.application.event;

import io.jgitkins.server.shared.domain.event.DomainEvent;

import java.util.List;

public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
