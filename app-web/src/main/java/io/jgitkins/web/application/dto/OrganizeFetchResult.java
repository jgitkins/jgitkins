package io.jgitkins.web.application.dto;

import java.util.List;

public record OrganizeFetchResult(List<OrganizeSummary> organizes, String errorMessage) {
}
