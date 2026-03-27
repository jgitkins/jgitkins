package io.jgitkins.web.e2e;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.jgitkins.web.application.dto.UserCredentialIssueResult;
import io.jgitkins.web.application.port.in.PersonalAccessTokenIssueUseCase;
import io.jgitkins.web.application.port.in.PersonalAccessTokenQueryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"spring.profiles.active=test",
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.session.SessionAutoConfiguration",
		"JGITKINS_SERVER_BASE_URL=http://localhost:18084",
		"OAUTH_GOOGLE_CLIENT_ID=test-client",
		"OAUTH_GOOGLE_CLIENT_SECRET=test-secret"
})
@AutoConfigureMockMvc
class SettingsPersonalAccessTokenE2ETest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PersonalAccessTokenIssueUseCase issueUseCase;
	@MockBean
	private PersonalAccessTokenQueryUseCase queryUseCase;
	@MockBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void createPersonalAccessToken_validationFailure_returnsFormError() throws Exception {
		mockMvc.perform(post("/settings/personal-access-tokens")
						.with(user("tester"))
						.with(csrf())
						.param("name", "")
						.param("description", "desc")
						.param("expiration", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("settings/personal-access-tokens/new"))
				.andExpect(model().attribute("error", "All fields are required."));
	}

	@Test
	void createPersonalAccessToken_success_redirectsWithIssuedToken() throws Exception {
		when(issueUseCase.issueToken(any())).thenReturn(new UserCredentialIssueResult(1L, "jkpat_token"));

		mockMvc.perform(post("/settings/personal-access-tokens")
						.with(user("tester"))
						.with(csrf())
						.param("name", "ci-token")
						.param("description", "for ci")
						.param("expiration", "2026-12-31"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/settings/personal-access-tokens"))
				.andExpect(flash().attribute("issuedToken", "jkpat_token"));
	}
}
