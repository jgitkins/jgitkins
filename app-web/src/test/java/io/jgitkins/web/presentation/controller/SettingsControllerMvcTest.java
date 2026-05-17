package io.jgitkins.web.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.jgitkins.web.application.port.in.facade.SettingsFacadeUseCase;
import io.jgitkins.web.presentation.support.SettingsViewSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class SettingsControllerMvcTest {

	@Mock
	private SettingsFacadeUseCase settingsFacadeUseCase;
	@Mock
	private SettingsViewSupport settingsViewSupport;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		SettingsController controller = new SettingsController(settingsFacadeUseCase, settingsViewSupport);
		when(settingsViewSupport.getMessage("validation.pat.fields.required"))
				.thenReturn("All fields are required.");
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setValidator(validator)
				.build();
	}

	@Test
	void createPersonalAccessToken_returnsValidationErrorViewWhenRequiredFieldsMissing() throws Exception {
		mockMvc.perform(post("/settings/personal-access-tokens")
				.param("name", "")
				.param("description", "desc")
				.param("expiration", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("settings/personal-access-tokens/new"))
				.andExpect(model().attribute("error", "All fields are required."));
	}
}
