package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.dto.HomeDashboardSummary;
import io.jgitkins.web.application.dto.DashboardSummary;
import io.jgitkins.web.application.port.in.facade.DashboardFacadeUseCase;
import io.jgitkins.web.application.port.in.facade.HomeFacadeUseCase;
import io.jgitkins.web.presentation.support.SessionSupport;
import io.jgitkins.web.presentation.support.UserDisplayNameResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HomeFacade implements HomeFacadeUseCase {

    private final DashboardFacadeUseCase dashboardFacadeUseCase;
    private final UserDisplayNameResolver userDisplayNameResolver;
    private final SessionSupport sessionSupport;

    @Override
    public HomeDashboardSummary getHomeViewData(Authentication authentication, HttpServletRequest request) {
        HttpSession session = sessionSupport.resolveSession(request);
        String username = sessionSupport.resolveUsername(session);

        // Dashboard 조립 (DashboardFacade 활용)
        DashboardSummary dashboardSummary = dashboardFacadeUseCase.getDashboardSummary(username);

        // 기타 정보 추출
        String displayName = userDisplayNameResolver.resolve(authentication);
        boolean pendingUsername = sessionSupport.isPendingUsername(session);
        String usernameSetupError = sessionSupport.popUsernameSetupError(session);

        return new HomeDashboardSummary(
                dashboardSummary,
                displayName,
                pendingUsername,
                usernameSetupError,
                LocalDateTime.now());
    }
}
