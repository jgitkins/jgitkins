package io.jgitkins.server.repository.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA mapping for {@code REPOSITORY_MEMBER}.
 *
 * <p>{@code ADDED_AT} stays writable, unlike the database-owned timestamps on {@code BRANCH}:
 * {@code RepositoryMember.create} already defaults it to now when a caller omits it, so the
 * application, not the column default, has always decided the value.
 *
 * <p>{@code ROLE} is the enum name as a varchar rather than {@code @Enumerated}, matching what the
 * MyBatis mapper stored. {@code @Enumerated(ORDINAL)} would silently reinterpret existing rows and
 * {@code @Enumerated(STRING)} would tie the stored form to the enum's declaration.
 */
@Entity
@Table(name = "REPOSITORY_MEMBER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class RepositoryMemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "REPOSITORY_ID", nullable = false)
    private Long repositoryId;

    @Column(name = "USER_ID", nullable = false)
    private Long userId;

    @Column(name = "ROLE", nullable = false, length = 32)
    private String role;

    @Column(name = "ADDED_AT", nullable = false)
    private LocalDateTime addedAt;
}
