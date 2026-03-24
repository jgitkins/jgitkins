package io.jgitkins.server.domain.model.vo;

/**
 * Job 실행 상태 Value Object
 * 서버 관점에서는 PENDING, IN_QUEUE만 직접 관리
 */
public enum JobStatus {
    PENDING, // 생성됨, 대기 중
    IN_QUEUE, // 큐에 등록됨
    IN_PROGRESS, // 실행 중 (Runner 관점)
    SUCCESS, // 성공 (Runner 관점)
    FAILED, // 실패 (Runner 관점)
    CANCELED; // 취소됨

    /**
     * 종료 상태인지 확인
     */
    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELED;
    }

    /**
     * 대기 상태인지 확인
     */
    public boolean isWaiting() {
        return this == PENDING || this == IN_QUEUE;
    }

    /**
     * 실행 중 상태인지 확인
     */
    public boolean isRunning() {
        return this == IN_PROGRESS;
    }

    /**
     * 성공 상태인지 확인
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 실패 상태인지 확인
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}
