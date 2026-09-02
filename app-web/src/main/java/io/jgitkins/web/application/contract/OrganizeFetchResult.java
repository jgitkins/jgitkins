package io.jgitkins.web.application.contract;

import java.util.List;

public record OrganizeFetchResult(List<OrganizeSummary> organizes, String errorMessage) {
}
