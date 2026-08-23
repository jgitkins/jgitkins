package io.jgitkins.server.collaboration.adapter.in.support;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RequesterUserIdResolver {

    public Optional<Long> resolve(String subject) {
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(subject));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}