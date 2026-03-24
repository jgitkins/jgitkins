package io.jgitkins.web.presentation.support;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;

@Component
public class RepositoryTreePathSupport {

	public String resolveTreeDirectory(HttpServletRequest request) {
		String pathWithinMapping = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (!StringUtils.hasText(pathWithinMapping) || !StringUtils.hasText(bestMatchPattern)) {
			return "";
		}
		AntPathMatcher matcher = new AntPathMatcher();
		String extracted = matcher.extractPathWithinPattern(bestMatchPattern, pathWithinMapping);
		return trimSlashes(extracted);
	}

	private String trimSlashes(String value) {
		if (!StringUtils.hasText(value)) {
			return "";
		}
		return value.replaceAll("^/+", "").replaceAll("/+$", "");
	}
}
