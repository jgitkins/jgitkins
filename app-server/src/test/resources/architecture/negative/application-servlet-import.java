package io.jgitkins.server.repository.application.service;

// Two independently mapped categories in one fixture: servlet and Spring Security. The pair is the
// assertion unit, so this file must produce exactly one match for each.
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

public class ApplicationServletImport {
    HttpServletRequest request;

    void read() {
        SecurityContextHolder.getContext();
    }
}
