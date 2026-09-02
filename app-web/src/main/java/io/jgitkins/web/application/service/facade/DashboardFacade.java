package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.contract.CommitSummary;
import io.jgitkins.web.application.contract.DashboardData;
import io.jgitkins.web.application.contract.DashboardFeedItem;
import io.jgitkins.web.application.contract.DashboardRepoItem;
import io.jgitkins.web.application.contract.DashboardSummary;
import io.jgitkins.web.application.contract.RepositoryCommits;
import io.jgitkins.web.application.port.in.DashboardUseCase;
import io.jgitkins.web.application.port.in.facade.DashboardFacadeUseCase;
import io.jgitkins.web.infrastructure.config.JgitkinsWebProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardFacade implements DashboardFacadeUseCase {

    private final DashboardUseCase dashboardUseCase;
    private final JgitkinsWebProperties webProperties;

    @Override
    public DashboardSummary getDashboardSummary(String username) {
        DashboardData data = dashboardUseCase.buildDashboardForUser(username);
        return toDashboardSummary(data);
    }

    private DashboardSummary toDashboardSummary(DashboardData data) {
        List<DashboardRepoItem> items = new ArrayList<>();
        List<DashboardFeedItem> feed = new ArrayList<>();

        for (RepositoryCommits repository : data.repositories()) {
            String ownerSlug = resolveOwnerSlug(repository.namespace());
            items.add(new DashboardRepoItem(
                    repository.namespace(),
                    ownerSlug,
                    repository.repoName(),
                    repository.repository(),
                    repository.commits(),
                    0,
                    0,
                    buildLink(webProperties.prUrlTemplate(), repository),
                    buildLink(webProperties.issueUrlTemplate(), repository)));

            for (CommitSummary commit : repository.commits()) {
                if (commit.commitTime() != null) {
                    feed.add(new DashboardFeedItem(
                            repository.namespace(),
                            repository.repoName(),
                            commit.shortMessage(),
                            commit.authorName(),
                            commit.commitTime()));
                }
            }
        }

        feed.sort(Comparator.comparing(DashboardFeedItem::commitTime).reversed());
        if (feed.size() > 10) {
            feed = feed.subList(0, 10);
        }

        return new DashboardSummary(
                data.organizes(),
                items,
                feed,
                data.errorMessage());
    }

    private String buildLink(String template, RepositoryCommits repository) {
        if (!StringUtils.hasText(template)) {
            return null;
        }
        return template
                .replace("{namespace}", repository.namespace())
                .replace("{repo}", repository.repoName());
    }

    private String resolveOwnerSlug(String namespace) {
        if (!StringUtils.hasText(namespace)) {
            return "unknown";
        }
        String trimmed = namespace.replaceAll("/+$", "");
        int index = trimmed.lastIndexOf('/');
        if (index < 0) {
            return trimmed;
        }
        return trimmed.substring(index + 1);
    }
}
