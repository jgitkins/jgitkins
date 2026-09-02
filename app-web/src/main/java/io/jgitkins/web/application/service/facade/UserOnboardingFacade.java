package io.jgitkins.web.application.service.facade;

import io.jgitkins.web.application.contract.UsernameUpdateResult;
import io.jgitkins.web.application.port.in.facade.UserOnboardingFacadeUseCase;
import io.jgitkins.web.application.port.out.UserPort;
import io.jgitkins.web.presentation.support.SessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserOnboardingFacade implements UserOnboardingFacadeUseCase {

    private final UserPort userPort;
    private final SessionSupport sessionSupport;

    @Override
    public UsernameUpdateResult setupUsername(String username, HttpServletRequest request) {
        UsernameUpdateResult result = userPort.updateUsername(username);

        if (result.errorMessage() == null) {
            sessionSupport.storeUsername(request, username);
            sessionSupport.activateUser(request);
        } else {
            sessionSupport.storeUsernameSetupError(request, result.errorMessage());
        }

        return result;
    }
}
