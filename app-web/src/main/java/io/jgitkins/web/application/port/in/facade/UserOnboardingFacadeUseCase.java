package io.jgitkins.web.application.port.in.facade;

import io.jgitkins.web.application.dto.UsernameUpdateResult;
import jakarta.servlet.http.HttpServletRequest;

public interface UserOnboardingFacadeUseCase {

    UsernameUpdateResult setupUsername(String username, HttpServletRequest request);

}
