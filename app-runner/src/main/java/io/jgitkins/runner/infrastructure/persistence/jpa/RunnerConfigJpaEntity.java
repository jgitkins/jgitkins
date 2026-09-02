package io.jgitkins.runner.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA mapping for the RUNNER_CONFIG table: the runner's scalar settings, one row per key.
 *
 * <p>{@code (RUNNER_ID, CONFIG_KEY)} is unique in the table, which is what makes the adapter's
 * read-then-insert-or-update safe to express as two statements: a second row for the same key cannot
 * exist, so the read either finds the one row to update or finds nothing.
 *
 * <p>The runner-to-config relation is left as a plain {@code RUNNER_ID} column rather than a
 * {@code @ManyToOne}. Nothing navigates from a config row to its runner -- the adapter always starts
 * from the runner -- and an association would add a lazy proxy and a second select to a store that
 * holds exactly one runner.
 */
@Entity
@Table(name = "RUNNER_CONFIG")
public class RunnerConfigJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUNNER_ID", nullable = false)
    private Long runnerId;

    @Column(name = "CONFIG_KEY", nullable = false, length = 128)
    private String configKey;

    @Column(name = "CONFIG_VALUE", nullable = false)
    private String configValue;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunnerId() {
        return runnerId;
    }

    public void setRunnerId(Long runnerId) {
        this.runnerId = runnerId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
