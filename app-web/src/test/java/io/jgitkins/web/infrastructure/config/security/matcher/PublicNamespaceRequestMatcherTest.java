package io.jgitkins.web.infrastructure.config.security.matcher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicNamespaceRequestMatcherTest {

	private final PublicNamespaceRequestMatcher matcher = new PublicNamespaceRequestMatcher();

	@Test
	void matches_publicNamespacePatterns() {
		assertTrue(matcher.matches(request("/alice")));
		assertTrue(matcher.matches(request("/alice/repo")));
		assertTrue(matcher.matches(request("/alice/-/repositories")));
		assertTrue(matcher.matches(request("/team/backend-repo")));
	}

	@Test
	void doesNotMatch_reservedOrProtectedPatterns() {
		assertFalse(matcher.matches(request("/settings")));
		assertFalse(matcher.matches(request("/explore")));
		assertFalse(matcher.matches(request("/repositories/new")));
		assertFalse(matcher.matches(request("/actuator/prometheus")));
		assertFalse(matcher.matches(request("/assets/app.css")));
	}

	@Test
	void matches_deepPathWithTreeOrFindFilesRoute() {
		assertTrue(matcher.matches(request("/alice/repo/tree")));
		assertTrue(matcher.matches(request("/alice/repo/tree/src")));
	}

	private MockHttpServletRequest request(String uri) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI(uri);
		return request;
	}
}
