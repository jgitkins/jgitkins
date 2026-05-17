package io.jgitkins.web.infrastructure.config.security.matcher;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;

public class PublicNamespaceRequestMatcher implements RequestMatcher {

	private static final Set<String> RESERVED_SEGMENTS = Set.of(
			"assets", "css", "js", "img", "svg", "favicon.ico", "webjars",
			"settings", "notifications", "explore", "fragments", "repositories",
			"oauth2", "login", "error", "actuator");

	@Override
	public boolean matches(HttpServletRequest request) {
		String path = normalizePath(request);
		if (!StringUtils.hasText(path) || "/".equals(path)) {
			return false;
		}

		List<String> segments = toSegments(path);
		if (segments.isEmpty()) {
			return false;
		}

		String firstSegment = segments.get(0).toLowerCase(Locale.ROOT);
		if (RESERVED_SEGMENTS.contains(firstSegment)) {
			return false;
		}

		if (segments.size() == 1) {
			return true;
		}
		if (segments.size() == 2) {
			return true;
		}
		if (segments.size() >= 3) {
			String secondSegment = segments.get(1);
			String thirdSegment = segments.get(2).toLowerCase(Locale.ROOT);
			return "-".equals(secondSegment) || "tree".equals(thirdSegment) || "find-files".equals(thirdSegment);
		}
		return false;
	}

	private String normalizePath(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String uri = request.getRequestURI();
		if (!StringUtils.hasText(uri)) {
			return null;
		}
		String contextPath = request.getContextPath();
		if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
			uri = uri.substring(contextPath.length());
		}
		return uri;
	}

	private List<String> toSegments(String path) {
		return List.of(path.split("/"))
				.stream()
				.filter(StringUtils::hasText)
				.collect(Collectors.toList());
	}
}
