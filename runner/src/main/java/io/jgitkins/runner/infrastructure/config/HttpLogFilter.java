//package io.jgitkins.runner.infrastructure.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Slf4j
//@Component
//public class HttpLogFilter extends OncePerRequestFilter {
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
////        HttpServletRequest request = (HttpServletRequest) servletRequest;
//        log.debug("HTTP Method: [{}], URI: [{}], SevletPath: [{}], PathInfo: [{}]", request.getMethod(), request.getRequestURI(), request.getServletPath(), request.getPathInfo());
//        filterChain.doFilter(request, response);
//
//    }
//}
