package io.jgitkins.server.common.infrastructure.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
import io.jgitkins.server.common.infrastructure.config.filter.GitSmartHttpAuthFilter;
import io.jgitkins.server.common.infrastructure.config.security.handler.ApiAccessDeniedHandler;
import io.jgitkins.server.common.infrastructure.config.security.handler.ApiAnauthorizeHandler;
import io.jgitkins.server.common.infrastructure.config.security.handler.OAuth2LoginSuccessHandler;
import io.jgitkins.server.identity.access.application.port.in.OAuthLoginUseCase;
import io.jgitkins.server.identity.access.adapter.in.security.JwtAuthenticationFilter;
import io.jgitkins.server.identity.access.application.service.JwtAuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain gitSecurityFilterChain(HttpSecurity http,
                                               GitSmartHttpAuthFilter gitSmartHttpAuthFilter) throws Exception {
        http.securityMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/git/**"),
                new AntPathRequestMatcher("/**/*.git"),
                new AntPathRequestMatcher("/**/*.git/**")
        ));
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.addFilterBefore(gitSmartHttpAuthFilter, BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               OAuth2LoginSuccessHandler successHandler,
                                               JwtAuthenticationFilter jwtAuthenticationFilter,
                                               ApiAnauthorizeHandler apiAnauthorizeHandler,
                                               ApiAccessDeniedHandler apiAccessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/oauth2/**", "/login/**", "/swagger-ui/**", "/actuator/prometheus", "/v3/api-docs/**")
                .permitAll()
                .anyRequest().permitAll());
        http.oauth2Login(oauth2 -> oauth2.successHandler(successHandler));
        http.oauth2Client(Customizer.withDefaults());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(apiAnauthorizeHandler)
                .accessDeniedHandler(apiAccessDeniedHandler));
        return http.build();
    }

    @Bean
    OAuth2LoginSuccessHandler oauth2LoginSuccessHandler(ObjectMapper objectMapper,
                                                        OAuthLoginUseCase oauthLoginUseCase) {
        return new OAuth2LoginSuccessHandler(objectMapper, oauthLoginUseCase);
    }

    @Bean
    JwtAuthenticationFilter jwtAuthenticationFilter(JwtAuthService jwtAuthService,
                                                    ApiAnauthorizeHandler apiAnauthorizeHandler) {
        return new JwtAuthenticationFilter(jwtAuthService, apiAnauthorizeHandler);
    }

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper) {
        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    ApiAnauthorizeHandler anauthorizeHandler(SecurityErrorResponseWriter responseWriter) {
        return new ApiAnauthorizeHandler(responseWriter);
    }

    @Bean
    ApiAccessDeniedHandler accessDeniedHandler(SecurityErrorResponseWriter responseWriter) {
        return new ApiAccessDeniedHandler(responseWriter);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
