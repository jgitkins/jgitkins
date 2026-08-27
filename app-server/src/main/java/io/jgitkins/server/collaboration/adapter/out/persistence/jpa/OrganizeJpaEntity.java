package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA mapping for the ORGANIZE table.
 *
 * <p>Lives under {@code adapter/out/persistence/jpa} on purpose. Task 2.66 forbids
 * {@code jakarta.persistence} imports under the domain and application roots, and the outbound
 * persistence adapter is outside those roots, so the technology stays where the boundary allows it.
 *
 * <p>Column set is taken from {@code app-server/data/ddl.sql}, not invented: the migration must not
 * change the schema, so this maps the table exactly as MyBatis already sees it. {@code PATH} is
 * {@code NOT NULL UNIQUE} in the table and the MyBatis mapper derives it from the organization
 * name, so the JPA adapter derives it the same way. Getting that wrong would surface as a unique
 * constraint violation only under the jpa selector, which is the kind of divergence the whole
 * selector mechanism exists to make detectable.
 */
@Entity
@Table(name = "ORGANIZE")
public class OrganizeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "PATH", nullable = false)
    private String path;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    protected OrganizeJpaEntity() {
        // required by JPA
    }

    public OrganizeJpaEntity(Long id, String name, String path, String description, Long ownerId,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.description = description;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    void apply(String name, String path, String description, Long ownerId, LocalDateTime updatedAt) {
        this.name = name;
        this.path = path;
        this.description = description;
        this.ownerId = ownerId;
        this.updatedAt = updatedAt;
    }
}
