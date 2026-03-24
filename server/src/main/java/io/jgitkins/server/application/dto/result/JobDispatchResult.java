package io.jgitkins.server.application.dto.result;

import java.time.LocalDateTime;

public record JobDispatchResult(Long jobId,
                                Long jobHistoryId,
                                Long runnerId,
                                Long repositoryId,
                                Long organizeId,
                                String commitHash,
                                String branchName,
                                Long triggeredBy,
                                LocalDateTime dispatchedAt,
                                String cloneUrl) {
}
