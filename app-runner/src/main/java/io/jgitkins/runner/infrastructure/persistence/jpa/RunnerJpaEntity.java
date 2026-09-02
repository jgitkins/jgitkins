package io.jgitkins.runner.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA mapping for the RUNNER table.
 *
 * <p>Column set is taken from {@code app-runner/src/main/resources/DDL.sql}, not invented. This
 * replaces the MyBatis-generated {@code RunnerEntity} one-for-one: same table, same columns, same
 * nullability, so the runner's existing {@code ~/runner.mv.db} file keeps working across the switch
 * without a migration. A runner that has already been activated must come back activated.
 *
 * <p>{@code NAME} is {@code NOT NULL} in the table but the runner never had a name to store -- the
 * MyBatis path wrote an empty string through a MapStruct constant, and that is preserved rather than
 * quietly turned into something more meaningful, because the server is what names runners.
 */
@Entity
@Table(name = "RUNNER")
public class RunnerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TOKEN", nullable = false, length = 128)
    private String token;

    @Column(name = "NAME", nullable = false, length = 128)
    private String name;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
