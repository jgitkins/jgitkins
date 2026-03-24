package io.jgitkins.web.application.dto;

import java.util.List;

public record ExploreSummary(
        String exploreType,
        List<ExploreRepoSummary> repositories,
        List<OrganizeSummary> organizations,
        List<UserSummary> users) {
}
