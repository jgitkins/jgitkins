package io.jgitkins.server.repository.application.service;

import org.springframework.security.core.context.SecurityContextHolder;

public class ApplicationSecurityImport {
    void read() {
        SecurityContextHolder.getContext();
    }
}
