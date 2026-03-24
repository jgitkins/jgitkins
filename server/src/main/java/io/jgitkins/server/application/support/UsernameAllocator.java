package io.jgitkins.server.application.support;

import io.jgitkins.server.application.port.out.OrganizePersistencePort;
import io.jgitkins.server.application.port.out.UserPersistencePort;
import io.jgitkins.server.domain.model.vo.OrganizeName;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernameAllocator {

	private final UserPersistencePort userPort;
	private final OrganizePersistencePort organizePort;

	public String deriveBaseUsername(String email, String providerName, String providerSub) {
		// TODO: email 등 문자열 기본 검증 책임은 호출자(Controller 또는 상위 Service)로 이전
		String base = (email != null && !email.isBlank()) ? email.split("@")[0] : providerName + "-" + providerSub;
		return sanitize(base.toLowerCase());
	}

	public String allocateUniqueUsername(String baseUsername, String providerSub) {
		String providerSuffix = providerSub == null ? "user"
				: providerSub.substring(Math.max(0, providerSub.length() - 6));
		String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

		String[] candidates = new String[] {
				baseUsername,
				baseUsername + "-" + providerSuffix.toLowerCase(),
				baseUsername + "-" + randomSuffix.toLowerCase(),
				baseUsername + "-" + System.currentTimeMillis()
		};

		for (String candidate : candidates) {
			if (isNamespaceAvailable(candidate)) {
				return candidate;
			}
		}

		return baseUsername + "-" + System.nanoTime();
	}

	private String sanitize(String value) {
		return value.replaceAll("[^a-z0-9._-]", "-");
	}

	private boolean isNamespaceAvailable(String username) {
		if (userPort.findByUsername(username).isPresent()) {
			return false;
		}
		return findOrganizeByName(username).isEmpty();
	}

	private Optional<io.jgitkins.server.domain.aggregate.Organize> findOrganizeByName(String namespace) {
		try {
			return organizePort.findByName(OrganizeName.from(namespace));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
