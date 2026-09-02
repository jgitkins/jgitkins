package io.jgitkins.runner.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA mapping for the RUNNER_CONFIG_FILE table: the runner's file-shaped settings.
 *
 * <p>"File" is what the table calls it; nothing is written to disk. {@code FILENAME} holds a
 * configuration key and {@code CONTENTS} holds a value that is expected to be long enough to want a
 * {@code TEXT} column -- today the container image name and the Jenkins plugin configuration. The
 * split from RUNNER_CONFIG is therefore about value size, not about meaning, which is why the adapter
 * folds both tables into one key-to-value map before handing it to the domain mapper.
 *
 * <p>Same reasoning as {@link RunnerConfigJpaEntity} for the plain {@code RUNNER_ID} column and for
 * relying on the table's {@code (RUNNER_ID, FILENAME)} uniqueness.
 */
@Entity
@Table(name = "RUNNER_CONFIG_FILE")
public class RunnerConfigFileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUNNER_ID", nullable = false)
    private Long runnerId;

    @Column(name = "FILENAME", nullable = false, length = 255)
    private String filename;

    @Column(name = "CONTENTS", nullable = false)
    private String contents;

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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
