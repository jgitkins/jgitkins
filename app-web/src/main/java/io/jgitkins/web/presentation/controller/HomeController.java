package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.dto.HomeDashboardSummary;
import io.jgitkins.web.application.port.in.facade.HomeFacadeUseCase;
import io.jgitkins.web.presentation.support.HomeViewSupport;
import io.jgitkins.web.presentation.support.SecuritySupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

	private final HomeFacadeUseCase homeFacadeUseCase;
	private final HomeViewSupport homeViewSupport;
	private final SecuritySupport securitySupport;

	@GetMapping("/")
	public String root(Authentication authentication, Model model, HttpServletRequest request) {
		if (securitySupport.isAuthenticated(authentication)) {
			HomeDashboardSummary homeData = homeFacadeUseCase.getHomeViewData(authentication, request);
			homeViewSupport.populateModel(model, homeData);
			return "dashboard/index";
		}
		return "index";
	}

	@GetMapping("/login")
	public String login() {
		return "redirect:/oauth2/authorization/google";
	}
}
