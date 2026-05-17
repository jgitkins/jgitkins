package io.jgitkins.server.application.dto.result;

import io.jgitkins.server.change.review.domain.model.BranchHeadSnapshot;
import io.jgitkins.server.change.review.domain.model.PullRequestStatus;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PullRequestResult {

    private final Long id;
    private final Long repositoryId;
    private final BranchHeadSnapshot source;
    private final BranchHeadSnapshot target;
    private final PullRequestStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
