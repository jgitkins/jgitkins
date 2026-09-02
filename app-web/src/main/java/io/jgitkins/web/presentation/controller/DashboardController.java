package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.contract.DashboardSummary;
import io.jgitkins.web.application.port.in.facade.DashboardFacadeUseCase;
import io.jgitkins.web.presentation.support.SessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/fragments")
@RequiredArgsConstructor
public class DashboardController {

	private final DashboardFacadeUseCase dashboardFacadeUseCase;
	private final SessionSupport sessionSupport;

	@GetMapping("/organizes")
	public String organizeFragment(Model model, HttpServletRequest request) {
		DashboardSummary summary = dashboardFacadeUseCase.getDashboardSummary(resolveUsername(request));
		model.addAttribute("dashboard", summary);
		model.addAttribute("lastUpdated", LocalDateTime.now());
		return "fragments/organizes :: list";
	}

	@GetMapping("/feed")
	public String feedFragment(Model model, HttpServletRequest request) {
		DashboardSummary summary = dashboardFacadeUseCase.getDashboardSummary(resolveUsername(request));
		model.addAttribute("dashboard", summary);
		return "fragments/feed :: list";
	}

	private String resolveUsername(HttpServletRequest request) {
		return sessionSupport.resolveUsername(sessionSupport.resolveSession(request));
	}
}
