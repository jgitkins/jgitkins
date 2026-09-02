package io.jgitkins.web.presentation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.contract.UserCredentialIssueResult;
import io.jgitkins.web.application.port.in.facade.SettingsFacadeUseCase;
import io.jgitkins.web.presentation.contract.PersonalAccessTokenForm;
import io.jgitkins.web.presentation.support.SettingsViewSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

	@Mock
	private SettingsFacadeUseCase settingsFacadeUseCase;
	@Mock
	private SettingsViewSupport settingsViewSupport;

	private SettingsController controller;

	@BeforeEach
	void setUp() {
		controller = new SettingsController(settingsFacadeUseCase, settingsViewSupport);
	}

	@Test
	void createPersonalAccessToken_passesExpirationToIssueRequest() {
		PersonalAccessTokenForm form = new PersonalAccessTokenForm();
		form.setName("ci-token");
		form.setDescription("for ci");
		form.setExpiration("2026-12-31");
		BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
		Model model = new ConcurrentModel();
		RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

		when(settingsViewSupport.toIssueRequest(any())).thenReturn(null);
		when(settingsFacadeUseCase.issueToken(any())).thenReturn(new UserCredentialIssueResult(1L, "jkpat_token"));

		String view = controller.createPersonalAccessToken(form, bindingResult, model, redirect);

		assertEquals("redirect:/settings/personal-access-tokens", view);
		assertEquals("jkpat_token", redirect.getFlashAttributes().get("issuedToken"));
		verify(settingsFacadeUseCase).issueToken(any());
	}

	@Test
	void createPersonalAccessToken_rejectsWhenExpirationMissing() {
		PersonalAccessTokenForm form = new PersonalAccessTokenForm();
		BindingResult bindingResult = new BeanPropertyBindingResult(form, "form");
		bindingResult.rejectValue("expiration", "NotBlank", "All fields are required.");
		Model model = new ConcurrentModel();
		RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
		when(settingsViewSupport.getMessage("validation.pat.fields.required")).thenReturn("All fields are required.");

		String view = controller.createPersonalAccessToken(form, bindingResult, model, redirect);

		verify(settingsFacadeUseCase, never()).issueToken(any());
		assertEquals("settings/personal-access-tokens/new", view);
		assertSame("All fields are required.", model.getAttribute("error"));
	}
}
