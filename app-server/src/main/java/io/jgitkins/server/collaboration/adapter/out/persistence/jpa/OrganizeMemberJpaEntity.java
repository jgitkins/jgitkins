package io.jgitkins.server.collaboration.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA mapping for the ORGANIZE_MEMBER table.
 *
 * <p>Modelled as a flat row with {@code ORGANIZE_ID} as a plain column rather than a
 * {@code @ManyToOne} association. The MyBatis implementation reads and writes membership rows
 * without loading the parent organization, and an association here would introduce lazy loading and
 * a different query shape under the jpa selector. Cutover must not change behaviour, so the mapping
 * stays as close to the existing access pattern as the technology allows.
 */
@Entity
@Table(name = "ORGANIZE_MEMBER")
public class OrganizeMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "ORGANIZE_ID", nullable = false)
    private Long organizeId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ROLE", nullable = false, length = 32)
    private String role;

    @Column(name = "JOINED_AT", nullable = false)
    private LocalDateTime joinedAt;

    protected OrganizeMemberJpaEntity() {
        // required by JPA
    }

    public OrganizeMemberJpaEntity(Long id, Long organizeId, Long userId, String role, LocalDateTime joinedAt) {
        this.id = id;
        this.organizeId = organizeId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizeId() {
        return organizeId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
}
