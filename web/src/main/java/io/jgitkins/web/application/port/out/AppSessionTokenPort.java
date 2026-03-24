package io.jgitkins.web.application.port.out;

import jakarta.servlet.http.HttpServletRequest;

public interface AppSessionTokenPort {
	String getCurrentSessionToken();

	void store(HttpServletRequest request, String token);
}
