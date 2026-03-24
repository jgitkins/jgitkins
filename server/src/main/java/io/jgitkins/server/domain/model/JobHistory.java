package io.jgitkins.server.domain.model;

import io.jgitkins.server.domain.model.vo.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * JobHistory Entity (Aggregate 내부)
 * Job 실행 이력 추적
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class JobHistory {

    private final JobHistoryId id;
    private final JobId jobId;
    private final SequenceNumber seqNo;
    private final RunnerId runnerId; // PENDING, IN_QUEUE 단계에서는 null
    private final JobStatus status;
    private final SystemUser createdBy; // PENDING 단계에서는 SYSTEM
    private final LocalDateTime createdAt;

    /**
     * PENDING 상태 History 생성 (Factory Method)
     */
    public static JobHistory createPending(JobId jobId, LocalDateTime createdAt) {
        return new JobHistory(
                JobHistoryId.generate(),
                jobId,
                SequenceNumber.first(), // seq_no = 1
                null, // runner_id는 null
                JobStatus.PENDING,
                SystemUser.SYSTEM, // created_by = SYSTEM
                createdAt);
    }

    /**
     * IN_PROGRESS 상태 History 생성 (Factory Method)
     */
    public static JobHistory createInProgress(
            JobId jobId,
            int seqNo,
            RunnerId runnerId,
            LocalDateTime createdAt) {
        return new JobHistory(
                JobHistoryId.generate(),
                jobId,
                SequenceNumber.of(seqNo),
                runnerId,
                JobStatus.IN_PROGRESS,
                SystemUser.SYSTEM, // created_by = SYSTEM
                createdAt);
    }

    public static JobHistory createSuccess(
            JobId jobId,
            int seqNo,
            RunnerId runnerId,
            LocalDateTime createdAt) {
        return new JobHistory(
                JobHistoryId.generate(),
                jobId,
                SequenceNumber.of(seqNo),
                runnerId,
                JobStatus.SUCCESS,
                SystemUser.SYSTEM,
                createdAt);
    }

    public static JobHistory createFailed(
            JobId jobId,
            int seqNo,
            RunnerId runnerId,
            LocalDateTime createdAt) {
        return new JobHistory(
                JobHistoryId.generate(),
                jobId,
                SequenceNumber.of(seqNo),
                runnerId,
                JobStatus.FAILED,
                SystemUser.SYSTEM,
                createdAt);
    }

    /**
     * 재구성용 생성자 (Repository에서 사용)
     */
    public static JobHistory reconstruct(
            JobHistoryId id,
            JobId jobId,
            SequenceNumber seqNo,
            RunnerId runnerId,
            JobStatus status,
            SystemUser createdBy,
            LocalDateTime createdAt) {
        return new JobHistory(
                id,
                jobId,
                seqNo,
                runnerId,
                status,
                createdBy,
                createdAt);
    }

    /**
     * PENDING 또는 IN_QUEUE 상태인지 확인
     */
    public boolean isWaitingForRunner() {
        return status == JobStatus.PENDING || status == JobStatus.IN_QUEUE;
    }

    /**
     * Runner가 할당되었는지 확인
     */
    public boolean hasRunner() {
        return runnerId != null;
    }
}
