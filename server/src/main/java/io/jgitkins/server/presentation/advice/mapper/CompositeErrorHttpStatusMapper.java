package io.jgitkins.server.presentation.advice.mapper;

import io.jgitkins.server.common.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CompositeErrorHttpStatusMapper {

    private final List<ErrorHttpStatusMapper> delegates;

    public HttpStatus map(ErrorCode errorCode) {
        return delegates.stream()
                .filter(mapper -> mapper.supports(errorCode))
                .findFirst()
                .map(mapper -> mapper.map(errorCode))
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

