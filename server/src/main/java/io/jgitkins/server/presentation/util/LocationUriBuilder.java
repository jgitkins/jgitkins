package io.jgitkins.server.presentation.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

public class LocationUriBuilder {

    private LocationUriBuilder() {}

    public static URI create(Object... pathVars) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest() // 현재 URI 기준
                .path("/{path}")      // 일단 placeholder
                .buildAndExpand(pathVars)
                .toUri();
    }

}
