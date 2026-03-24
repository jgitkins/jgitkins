package io.jgitkins.web.infrastructure.adapter;

import io.jgitkins.web.application.common.SessionKeys;
import io.jgitkins.web.application.port.out.AppSessionTokenPort;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Stores app tokens in HttpSession; Spring Session persists the session to Redis/Valkey.
 */
@Component
public class SpringSessionAppTokenAdapter implements AppSessionTokenPort {

	@Override
	public String getCurrentSessionToken() {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (attributes == null) {
			return null;
		}
		HttpServletRequest request = attributes.getRequest();
		if (request == null || request.getSession(false) == null) {
			return null;
		}
		Object token = request.getSession(false).getAttribute(SessionKeys.APP_TOKEN);
		return token instanceof String ? (String) token : null;
	}

	@Override
	public void store(HttpServletRequest request, String token) {
		if (request == null || token == null) {
			return;
		}

        // Spring Session 설정에 의해 valkey에 적재됨
        // key 는 JSESSIONID, value 는 JWT 토큰을 의미
		request.getSession(true).setAttribute(SessionKeys.APP_TOKEN, token);
	}
}
