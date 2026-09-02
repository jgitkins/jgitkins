package io.jgitkins.web.presentation.support;

import io.jgitkins.web.application.contract.HomeDashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
public class HomeViewSupport {

	public void populateModel(Model model, HomeDashboardSummary data) {
		model.addAttribute("dashboard", data.dashboard());
		model.addAttribute("displayName", data.displayName());
		model.addAttribute("pendingUsername", data.pendingUsername());
		model.addAttribute("lastUpdated", data.lastUpdated());

		if (data.usernameSetupError() != null) {
			model.addAttribute("usernameSetupError", data.usernameSetupError());
		}
	}
}
