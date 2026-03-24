package io.jgitkins.web.infrastructure.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UrlPathHelper;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpLogFilter extends OncePerRequestFilter {

	private static final int MAX_BODY_LOG_LENGTH = 500;
	private static final List<String> SKIP_PREFIXES = List.of(
			"/assets/", "/css/", "/js/", "/img/", "/svg/", "/favicon", "/webjars/"
	);
	private static final String OMITTED = "[omitted]";

	private final Environment environment;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
		String method = requestWrapper.getMethod();
		String path = requestWrapper.getRequestURI();

		try {
			filterChain.doFilter(requestWrapper, responseWrapper);
		} finally {
			String requestBody = resolveLogBody(
					readBody(requestWrapper.getContentAsByteArray(), requestWrapper.getCharacterEncoding()),
					requestWrapper.getContentType()
			);
			String responseBody = resolveLogBody(
					readBody(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding()),
					responseWrapper.getContentType()
			);
			log.info("[SERVER] [HTTP] [METHOD {}] [PATH {}] [STATUS {}] [REQUEST_BODY {}] [RESPONSE_BODY {}]",
					method,
					path,
					responseWrapper.getStatus(),
					requestBody,
					responseBody);
			responseWrapper.copyBodyToResponse();
		}
	}

	private String readBody(byte[] body, String encoding) {
		if (body == null || body.length == 0) {
			return "";
		}
		try {
			Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
			return new String(body, charset);
		} catch (Exception ignored) {
			return new String(body, StandardCharsets.UTF_8);
		}
	}

	private String resolveLogBody(String body, String contentType) {
		if (!isBodyLoggingEnabled()) {
			return OMITTED;
		}
		if (!isLoggableContentType(contentType)) {
			return OMITTED;
		}
		return HttpLogSanitizer.sanitizeAndTruncate(body, MAX_BODY_LOG_LENGTH);
	}

	private boolean isBodyLoggingEnabled() {
		return environment.acceptsProfiles(Profiles.of("local"));
	}

	private boolean isLoggableContentType(String contentType) {
		if (contentType == null) {
			return false;
		}
		String normalized = contentType.toLowerCase();
		return normalized.contains("application/json")
				|| normalized.contains("application/x-www-form-urlencoded")
				|| normalized.startsWith("text/");
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = new UrlPathHelper().getPathWithinApplication(request);
		if (path == null) {
			return false;
		}
		for (String prefix : SKIP_PREFIXES) {
			if (path.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
