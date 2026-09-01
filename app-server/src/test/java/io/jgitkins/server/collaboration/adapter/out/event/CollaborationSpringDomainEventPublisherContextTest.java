package io.jgitkins.server.collaboration.adapter.out.event;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.collaboration.domain.aggregate.Organize;
import io.jgitkins.server.collaboration.domain.event.OrganizeCreatedEvent;
import io.jgitkins.server.collaboration.domain.vo.OrganizeId;
import io.jgitkins.server.collaboration.domain.vo.OrganizeName;
import io.jgitkins.server.collaboration.domain.vo.OwnerId;
import io.jgitkins.server.shared.domain.event.DomainEvent;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves the last hop. The sibling transaction test mocks {@code ApplicationEventPublisher}, so it
 * verifies the after-commit plumbing but not that anything ever arrives at a listener bean. Until
 * this test existed that hop had no coverage and no error handling: a listener that failed to
 * register would have been completely silent.
 *
 * <p>The listener is a plain {@code @EventListener}, not a transactional one, because the adapter
 * already defers to after-commit. Deferring twice would be the bug this test is meant to catch.
 *
 * <p>The context is assembled from explicit bean methods rather than a scan. A scan here would
 * pull the production configuration into a test that only needs four beans.
 */
@SpringJUnitConfig(CollaborationSpringDomainEventPublisherContextTest.TestConfig.class)
class CollaborationSpringDomainEventPublisherContextTest {

    @Autowired
    private CollaborationSpringDomainEventPublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RecordingListener listener;

    private DomainEvent event;

    @BeforeEach
    void setUp() {
        listener.received.clear();
        jdbcTemplate.execute("create table if not exists organize_test (id bigint primary key)");
        jdbcTemplate.execute("delete from organize_test");
        event = Organize.create(
                        OrganizeId.of(10L),
                        OrganizeName.from("core-team"),
                        OwnerId.of(7L),
                        "Core Team",
                        LocalDateTime.of(2026, 8, 14, 9, 0),
                        Instant.parse("2026-08-14T00:00:00Z"))
                .getDomainEvents()
                .get(0);
    }

    @Test
    void event_reachesARealListenerBeanAfterCommit() {
        transactionTemplate.execute(status -> {
            jdbcTemplate.update("insert into organize_test(id) values (1)");
            publisher.publish(List.of(event));
            assertThat(listener.received)
                    .as("nothing may reach a listener before the transaction commits")
                    .isEmpty();
            return null;
        });

        assertThat(listener.received).containsExactly(event);
        assertThat(jdbcTemplate.queryForObject("select count(*) from organize_test", Integer.class)).isEqualTo(1);
    }

    @Test
    void event_neverReachesARealListenerBeanAfterRollback() {
        transactionTemplate.execute(status -> {
            jdbcTemplate.update("insert into organize_test(id) values (1)");
            publisher.publish(List.of(event));
            status.setRollbackOnly();
            return null;
        });

        assertThat(listener.received).isEmpty();
        assertThat(jdbcTemplate.queryForObject("select count(*) from organize_test", Integer.class)).isZero();
    }

    static class RecordingListener {

        private final List<DomainEvent> received = new ArrayList<>();

        @EventListener
        void on(OrganizeCreatedEvent domainEvent) {
            received.add(domainEvent);
        }
    }

    @Configuration
    static class TestConfig {

        @Bean
        DataSource dataSource() {
            return new EmbeddedDatabaseBuilder()
                    .generateUniqueName(true)
                    .setType(EmbeddedDatabaseType.H2)
                    .build();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
            return new TransactionTemplate(transactionManager);
        }

        @Bean
        JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        CollaborationSpringDomainEventPublisher collaborationSpringDomainEventPublisher(
                ApplicationEventPublisher applicationEventPublisher) {
            return new CollaborationSpringDomainEventPublisher(applicationEventPublisher);
        }

        @Bean
        RecordingListener recordingListener() {
            return new RecordingListener();
        }
    }
}
