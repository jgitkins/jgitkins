package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.DashboardSummary;

public interface DashboardFacadeUseCase {

    DashboardSummary getDashboardSummary(String username);
}
