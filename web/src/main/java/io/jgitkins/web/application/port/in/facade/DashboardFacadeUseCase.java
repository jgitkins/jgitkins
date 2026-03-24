package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.dto.DashboardSummary;

public interface DashboardFacadeUseCase {

    DashboardSummary getDashboardSummary(String username);
}
