package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.contract.HomeDashboardSummary;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;

public interface HomeFacadeUseCase {

    HomeDashboardSummary getHomeViewData(Authentication authentication, HttpServletRequest request);

}
