package io.jgitkins.server.common.infrastructure.event;

import io.jgitkins.server.shared.application.event.DomainEventPublisher;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
