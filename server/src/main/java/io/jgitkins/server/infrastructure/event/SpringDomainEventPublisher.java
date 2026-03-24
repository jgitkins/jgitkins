package io.jgitkins.server.infrastructure.event;

import io.jgitkins.server.application.common.event.DomainEventPublisher;
import io.jgitkins.server.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publish(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        events.forEach(applicationEventPublisher::publishEvent);
    }
}
