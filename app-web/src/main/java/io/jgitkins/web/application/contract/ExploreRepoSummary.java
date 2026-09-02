package io.jgitkins.web.application.contract;

import java.time.LocalDateTime;

public record ExploreRepoSummary(
        String namespace,
        String repoName,
        String description,
        String visibility,
        LocalDateTime createdAt) {
}
