package io.jgitkins.server.infrastructure.adapter;

import io.jgitkins.server.application.port.out.PushEventRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PushEventRequestAdapter implements PushEventRequestResolver {

    @Override
    public Optional<Long> resolveRequesterId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Optional<Long> fromSecurityContext = parseUserId(authentication.getName());
            if (fromSecurityContext.isPresent()) {
                return fromSecurityContext;
            }
        }

        return getRequest().map(req -> parseUserId(req.getRemoteUser())).flatMap(opt -> opt);
    }

    private Optional<HttpServletRequest> getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? Optional.of(attributes.getRequest()) : Optional.empty();
    }

    private Optional<Long> parseUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(rawUserId));
        } catch (NumberFormatException ex) {
            log.warn("Unable to parse user id from request: {}", rawUserId);
            return Optional.empty();
        }
    }
}
