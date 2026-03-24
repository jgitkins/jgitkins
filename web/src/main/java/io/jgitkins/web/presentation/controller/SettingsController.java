package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.dto.UserCredentialIssueResult;
import io.jgitkins.web.application.port.in.facade.SettingsFacadeUseCase;
import io.jgitkins.web.presentation.dto.PersonalAccessTokenForm;
import io.jgitkins.web.presentation.support.SettingsViewSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class SettingsController {

	private final SettingsFacadeUseCase settingsFacadeUseCase;
	private final SettingsViewSupport settingsViewSupport;

	@GetMapping("/settings/profile")
	public String profile() {
		return "settings/profile";
	}

	@GetMapping("/settings")
	public String settingsRoot() {
		return "redirect:/settings/profile";
	}

	@GetMapping("/settings/personal-access-tokens")
	public String personalAccessTokens(Model model) {
		model.addAttribute("tokens", settingsFacadeUseCase.getPersonalAccessTokens());
		return "settings/personal-access-tokens/index";
	}

	@GetMapping("/settings/personal-access-tokens/new")
	public String newPersonalAccessToken() {
		return "settings/personal-access-tokens/new";
	}

	@PostMapping("/settings/personal-access-tokens")
	public String createPersonalAccessToken(@Valid @ModelAttribute PersonalAccessTokenForm form,
			BindingResult bindingResult,
			Model model,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", settingsViewSupport.getMessage("validation.pat.fields.required"));
			return "settings/personal-access-tokens/new";
		}

		try {
			UserCredentialIssueResult issued = settingsFacadeUseCase
					.issueToken(settingsViewSupport.toIssueRequest(form));
			redirectAttributes.addFlashAttribute("issuedToken", issued.token());
			return "redirect:/settings/personal-access-tokens";
		} catch (RuntimeException ex) {
			model.addAttribute("error", settingsViewSupport.getMessage("error.token.create_failed"));
			return "settings/personal-access-tokens/new";
		}
	}

	@PostMapping("/settings/personal-access-tokens/{credentialId}/delete")
	public String deletePersonalAccessToken(@PathVariable("credentialId") Long credentialId,
			Model model) {
		try {
			settingsFacadeUseCase.revokeToken(credentialId);
		} catch (RuntimeException ex) {
			model.addAttribute("error", settingsViewSupport.getMessage("error.token.delete_failed"));
			model.addAttribute("tokens", settingsFacadeUseCase.getPersonalAccessTokens());
			return "settings/personal-access-tokens/index";
		}
		return "redirect:/settings/personal-access-tokens";
	}
}
