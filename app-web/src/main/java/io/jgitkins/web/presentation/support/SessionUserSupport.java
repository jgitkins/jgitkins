package io.jgitkins.web.presentation.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.web.application.port.out.AppSessionTokenPort;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionUserSupport {

	private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() { };

	private final AppSessionTokenPort appSessionTokenPort;
	private final ObjectMapper objectMapper;

	public Optional<Long> resolveUserId() {
		String token = appSessionTokenPort.getCurrentSessionToken();
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		String[] parts = token.split("\\.");
		if (parts.length < 2) {
			return Optional.empty();
		}
		try {
			byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
			Map<String, Object> claims = objectMapper.readValue(payloadBytes, CLAIMS_TYPE);
			Object subject = claims.get("sub");
			if (subject == null) {
				return Optional.empty();
			}
			return Optional.of(Long.valueOf(subject.toString()));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}
}
