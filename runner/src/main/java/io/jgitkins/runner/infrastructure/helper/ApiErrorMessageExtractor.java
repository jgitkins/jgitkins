package io.jgitkins.runner.infrastructure.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiErrorMessageExtractor {

    private final ObjectMapper objectMapper;

    public Optional<String> extract(String body) {
        if (body == null || body.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode error = root.path("error");
            if (error.isObject()) {
                JsonNode message = error.path("message");
                if (message.isTextual()) {
                    return Optional.of(message.asText());
                }
            }
        } catch (JsonProcessingException ignored) {
        }
        return Optional.empty();
    }
}
