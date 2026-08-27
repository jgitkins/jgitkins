package io.jgitkins.server.execution.adapter.out.persistence.jpa;

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

/** JPA mapping for {@code RUNNER}. */
@Entity
@Table(name = "RUNNER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class RunnerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TOKEN", nullable = false, length = 255)
    private String token;

    @Column(name = "DESCRIPTION", length = 512)
    private String description;

    @Column(name = "STATUS", nullable = false, length = 32)
    private String status;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "LAST_HEARTBEAT_AT")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}
