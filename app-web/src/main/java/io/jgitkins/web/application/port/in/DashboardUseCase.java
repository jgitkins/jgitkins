package io.jgitkins.web.application.port.in;

import io.jgitkins.web.application.contract.DashboardData;

public interface DashboardUseCase {

	// DashboardData buildDashboard();

	DashboardData buildDashboardForUser(String username);
}
