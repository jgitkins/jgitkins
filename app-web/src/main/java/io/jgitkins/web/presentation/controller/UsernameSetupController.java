package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.port.in.facade.UserOnboardingFacadeUseCase;
import io.jgitkins.web.presentation.dto.UsernameSetupForm;
import io.jgitkins.web.presentation.support.UserOnboardingViewSupport;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UsernameSetupController {

	private final UserOnboardingFacadeUseCase userOnboardingFacadeUseCase;
	private final UserOnboardingViewSupport onboardingViewSupport;

	@GetMapping("/onboarding/username")
	public String usernameForm(Model model) {
		model.addAttribute("form", new UsernameSetupForm());
		return "onboarding/username";
	}

	@PostMapping("/onboarding/username")
	public String submitUsername(@ModelAttribute("form") UsernameSetupForm form,
			Model model,
			HttpServletRequest request) {
		if (form == null || !StringUtils.hasText(form.getUsername())) {
			onboardingViewSupport.storeError(request, onboardingViewSupport.getMessage("error.username.required"));
			return "redirect:/";
		}

		userOnboardingFacadeUseCase.setupUsername(form.getUsername(), request);
		return "redirect:/";
	}
}
