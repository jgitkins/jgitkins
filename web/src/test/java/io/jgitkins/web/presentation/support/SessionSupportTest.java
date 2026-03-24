package io.jgitkins.web.presentation.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.web.application.common.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SessionSupportTest {

	@Mock
	private HttpServletRequest request;
	@Mock
	private HttpSession session;

	private SessionSupport sessionSupport;

	@BeforeEach
	void setUp() {
		sessionSupport = new SessionSupport();
	}

	@Test
	void storeUsernameSetupError_createsSessionAndStoresMessage() {
		when(request.getSession(true)).thenReturn(session);

		sessionSupport.storeUsernameSetupError(request, "required");

		verify(request).getSession(true);
		verify(session).setAttribute(SessionKeys.USERNAME_SETUP_ERROR, "required");
	}

	@Test
	void activateUser_usesExistingSessionOnly() {
		when(request.getSession(false)).thenReturn(null);

		sessionSupport.activateUser(request);

		verify(request).getSession(false);
		verify(request, never()).getSession(true);
	}

	@Test
	void storeUserState_storesUsernameAndStatus() {
		when(request.getSession(true)).thenReturn(session);

		sessionSupport.storeUserState(request, "alzar", io.jgitkins.web.application.common.UserStatus.ACTIVE);

		verify(session).setAttribute(SessionKeys.USERNAME, "alzar");
		verify(session).setAttribute(SessionKeys.USER_STATUS, "ACTIVE");
	}

	@Test
	void popUsernameSetupError_returnsAndRemovesValue() {
		when(session.getAttribute(SessionKeys.USERNAME_SETUP_ERROR)).thenReturn("duplicated");

		String result = sessionSupport.popUsernameSetupError(session);

		assertEquals("duplicated", result);
		verify(session).removeAttribute(SessionKeys.USERNAME_SETUP_ERROR);
	}

	@Test
	void popUsernameSetupError_returnsNullWhenSessionMissing() {
		String result = sessionSupport.popUsernameSetupError(null);

		assertNull(result);
	}
}
