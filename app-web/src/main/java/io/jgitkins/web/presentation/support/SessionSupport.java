package io.jgitkins.web.presentation.support;

import io.jgitkins.web.application.common.SessionKeys;
import io.jgitkins.web.application.common.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class SessionSupport {

	public HttpSession resolveSession(HttpServletRequest request) {
		return request != null ? request.getSession(false) : null;
	}

	public HttpSession resolveOrCreateSession(HttpServletRequest request) {
		return request != null ? request.getSession(true) : null;
	}

	public String resolveUsername(HttpSession session) {
		if (session == null) {
			return null;
		}
		Object username = session.getAttribute(SessionKeys.USERNAME);
		return username instanceof String value ? value : null;
	}

	public boolean isPendingUsername(HttpSession session) {
		if (session == null) {
			return false;
		}
		Object status = session.getAttribute(SessionKeys.USER_STATUS);
		return UserStatus.isPending(status instanceof String ? (String) status : null);
	}

	public String popUsernameSetupError(HttpSession session) {
		if (session == null) {
			return null;
		}
		Object error = session.getAttribute(SessionKeys.USERNAME_SETUP_ERROR);
		if (error instanceof String errorMessage) {
			session.removeAttribute(SessionKeys.USERNAME_SETUP_ERROR);
			return errorMessage;
		}
		return null;
	}

	public void storeUsernameSetupError(HttpServletRequest request, String message) {
		setAttribute(resolveOrCreateSession(request), SessionKeys.USERNAME_SETUP_ERROR, message);
	}

	public void storeUsername(HttpServletRequest request, String username) {
		setAttribute(resolveOrCreateSession(request), SessionKeys.USERNAME, username);
	}

	public void activateUser(HttpServletRequest request) {
		setAttribute(resolveSession(request), SessionKeys.USER_STATUS, UserStatus.ACTIVE.name());
	}

	public void storeUserState(HttpServletRequest request, String username, UserStatus status) {
		HttpSession session = resolveOrCreateSession(request);
		setAttribute(session, SessionKeys.USERNAME, username);
		setAttribute(session, SessionKeys.USER_STATUS, status != null ? status.name() : null);
	}

	private void setAttribute(HttpSession session, String key, Object value) {
		if (session != null && key != null && value != null) {
			session.setAttribute(key, value);
		}
	}
}
