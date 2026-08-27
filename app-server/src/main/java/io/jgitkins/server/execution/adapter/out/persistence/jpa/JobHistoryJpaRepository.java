package io.jgitkins.server.execution.adapter.out.persistence.jpa;

import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data replacement for the {@code JOB_HISTORY} reads, including the one that carries the
 * compare-and-append lock.
 *
 * <p>{@link #lockLatestForJob} is the load-bearing method. It reproduces the hand-written mapper SQL
 * {@code ORDER BY CREATED_AT DESC, ID DESC LIMIT 1 FOR UPDATE}: the {@code @Lock} supplies
 * {@code FOR UPDATE} and the {@link Pageable} supplies {@code LIMIT 1}. Without the lock the
 * compare-before-append in {@code JobJpaRepositoryAdapter} degrades into a read-then-write race, and
 * two dispatchers can both observe the same latest history and both append — which is precisely the
 * double-dispatch the concurrency test exists to rule out.
 *
 * <p>It returns a list rather than an {@code Optional} because a locking query with a limit must
 * carry its {@code Pageable}, and a single-result projection would hide whether the limit was applied.
 */
public interface JobHistoryJpaRepository extends JpaRepository<JobHistoryJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from JobHistoryJpaEntity h where h.jobId = :jobId "
            + "order by h.createdAt desc, h.id desc")
    List<JobHistoryJpaEntity> lockLatestForJob(@Param("jobId") Long jobId, Pageable pageable);

    @Query("select h from JobHistoryJpaEntity h where h.jobId = :jobId "
            + "order by h.createdAt desc, h.id desc")
    List<JobHistoryJpaEntity> findLatestForJob(@Param("jobId") Long jobId, Pageable pageable);

    List<JobHistoryJpaEntity> findAllByJobIdOrderByCreatedAtAscIdAsc(Long jobId);
}
