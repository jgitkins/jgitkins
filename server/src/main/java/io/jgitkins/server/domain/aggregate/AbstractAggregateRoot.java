package io.jgitkins.server.domain.aggregate;

import io.jgitkins.server.domain.event.DomainEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base AggregateRoot implementation that records domain events in-memory
 * so application services can publish them after successful transactions.
 */
public abstract class AbstractAggregateRoot<ID> implements AggregateRoot<ID> {

    private final transient List<DomainEvent> domainEvents = new ArrayList<>();

    protected AbstractAggregateRoot() {
    }

    /**
     * Registers a new domain event. Aggregates should call this immediately
     * after state changes that need to be observed by other bounded contexts.
     *
     * EventListner 개념. 단, 순수 POJO로 직접 구현
     * 도메인 상태 변화 및 객체 기록만 책임
     * 기대효과: 애플리케이션 서비스가 트랜잭션 하기 직전에 aggregate.getDomainEvents를 읽어, 후속처리 진행 가능(메세지발행 등...)
     * 1. 도메인 로직 수행시 이벤트 적재
     * 2. 애플리케이션 서비스가 영속화를 모두 끝낸 뒤, 축적된 이벤트를 꺼내서 원하는 방식으로 처리
     * 3. (후속처리가 필요한경우에만)
     */
    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    /**
     * Copies domain events from an older aggregate instance. This is useful when
     * returning a new immutable instance while keeping already raised events.
     */
    protected void copyDomainEventsFrom(AbstractAggregateRoot<?> source) {
        if (source == null) {
            return;
        }
        domainEvents.addAll(source.domainEvents);
    }

    /**
     * Snapshot of events emitted since the aggregate was loaded.
     */
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clears recorded events once the application layer has published them.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
