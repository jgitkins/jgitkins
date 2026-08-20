package io.jgitkins.server.collaboration.adapter.out.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class CollaborationSpringDomainEventPublisherTransactionTest {

    private ApplicationEventPublisher applicationEventPublisher;
    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactionTemplate;
    private DomainEvent event;

    @BeforeEach
    void setUp() {
        var dataSource = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .build();
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table organize_test (id bigint primary key)");
        transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        applicationEventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
        event = Organize.create(
                OrganizeId.of(10L),
                OrganizeName.from("core-team"),
                OwnerId.of(7L),
                "Core Team",
                java.time.LocalDateTime.of(2026, 8, 14, 9, 0),
                Instant.parse("2026-08-14T00:00:00Z"))
                .getDomainEvents().get(0);
    }

    @Test
    void publish_deliversOnlyAfterSuccessfulCommit() {
        CollaborationSpringDomainEventPublisher publisher =
                new CollaborationSpringDomainEventPublisher(applicationEventPublisher);

        transactionTemplate.execute(status -> {
            jdbcTemplate.update("insert into organize_test(id) values (1)");
            publisher.publish(List.of(event));
            verify(applicationEventPublisher, never()).publishEvent(event);
            return null;
        });

        verify(applicationEventPublisher).publishEvent(event);
        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from organize_test", Integer.class));
    }

    @Test
    void publish_doesNotDeliverAfterRollback() {
        CollaborationSpringDomainEventPublisher publisher =
                new CollaborationSpringDomainEventPublisher(applicationEventPublisher);

        transactionTemplate.execute(status -> {
            jdbcTemplate.update("insert into organize_test(id) values (1)");
            publisher.publish(List.of(event));
            status.setRollbackOnly();
            return null;
        });

        verify(applicationEventPublisher, never()).publishEvent(event);
        assertEquals(0, jdbcTemplate.queryForObject("select count(*) from organize_test", Integer.class));
    }

    @Test
    void publish_keepsCommittedDataWhenPostCommitDeliveryFails() {
        CollaborationSpringDomainEventPublisher publisher =
                new CollaborationSpringDomainEventPublisher(applicationEventPublisher);
        doThrow(new IllegalStateException("listener failed"))
                .when(applicationEventPublisher).publishEvent(event);

        assertDoesNotThrow(() -> transactionTemplate.execute(status -> {
            jdbcTemplate.update("insert into organize_test(id) values (1)");
            publisher.publish(List.of(event));
            return null;
        }));

        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from organize_test", Integer.class));
    }
}
