package io.jgitkins.server.common.infrastructure.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.core.security.handler.SecurityErrorResponseWriter;
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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain gitSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(new OrRequestMatcher(
                new AntPathRequestMatcher("/git/**"),
                new AntPathRequestMatcher("/**/*.git"),
                new AntPathRequestMatcher("/**/*.git/**")
        ));
        http.csrf(csrf -> csrf.disable());
        // A fence, not an authentication design.
        //
        // Nothing serves these paths: no GitServlet or ServletRegistrationBean exists, and the two
        // pack factories that would drive one have zero consumers. Until this commit the chain was
        // permitAll, git authorization read its only identity from a client-supplied X-User-Id header,
        // and GitSmartHttpAuthFilter checked that an Authorization header was *present* without
        // parsing it -- because no httpBasic() was ever installed, so BasicAuthenticationFilter is a
        // position marker here rather than a filter. PatAuthenticationProvider and its BCrypt
        // verification are implemented and called by nobody.
        //
        // The threat is not "this is exploitable today" -- it is "wiring the servlet turns on a
        // git endpoint with no authentication, and every part named above makes it look like there
        // already is some". denyAll closes that absolutely: registering a servlet without
        // deliberately editing this line answers 403 to everything, loudly.
        //
        // Choosing this over installing httpBasic + wiring the PAT provider now is a decision about
        // *when* to design git authentication, not whether. PAT-over-Basic versus SSH keys, what
        // ROLE_GIT means, how a public repository's anonymous fetch stays anonymous -- those are
        // better settled with the servlet in hand than guessed at against dead code. Task 2.127-B
        // carries the follow-up.
        //
        // GitSmartHttpAuthFilter is deleted rather than left unregistered. With the chain denying
        // everything it could only do work for requests that cannot succeed -- and its first act was
        // a repository lookup, so an unauthenticated request to a path nothing serves was reaching
        // the database. Leaving an unregistered auth filter in the tree would also reproduce the
        // exact problem this commit is closing: a class that reads as a security mechanism and is
        // not one. Its rule worth keeping -- a public repository's fetch stays anonymous while a
        // receive-pack always challenges -- is recorded in task 2.127-B; the read half now lives in
        // GitSmartHttpAuthorizer, which resolves visibility itself.
        http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
        return http.build();
    }

    /**
     * The framework surfaces no controller serves.
     *
     * <p>Split out of the api chain so that the api chain's public list contains exactly the set the
     * route-classification guard can see. {@code RouteAuthenticationContractTest} enumerates
     * {@code RequestMappingHandlerMapping}; these four patterns are not in it, so while they lived in
     * the api chain's {@code requestMatchers} they were the part of the security configuration most
     * likely to be wrong and least likely to be caught.
     *
     * <p>{@code oauth2Login} and {@code oauth2Client} come with them. They install the filters that
     * serve {@code /oauth2/**} and the {@code /login/oauth2/code/{registrationId}} callback, and a
     * filter configured on a chain that does not match its own paths never runs. That path is not
     * exercised in the deployed topology -- browsers reach app-web -- but moving the paths without
     * the filters would turn working-but-unused code into silently broken code.
     *
     * <p>{@link InfraRoutes#MATCHER} is not optional. Without it this chain matches everything and
     * the api chain below never sees a request.
     */
    @Bean
    @Order(2)
    SecurityFilterChain infraSecurityFilterChain(HttpSecurity http,
                                                 OAuth2LoginSuccessHandler successHandler) throws Exception {
        http.securityMatcher(InfraRoutes.MATCHER);
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        http.oauth2Login(oauth2 -> oauth2.successHandler(successHandler));
        http.oauth2Client(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               JwtAuthenticationFilter jwtAuthenticationFilter,
                                               ApiAnauthorizeHandler apiAnauthorizeHandler,
                                               ApiAccessDeniedHandler apiAccessDeniedHandler) throws Exception {
        http.csrf(csrf -> csrf.disable());
        // No securityMatcher: this is the catch-all, and it must stay last.
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PublicApiRoutes.matchers()).permitAll()
                .anyRequest().permitAll());
        // Anonymous is disabled, and the reason it was disabled has since been removed.
        //
        // AnonymousAuthenticationFilter sets the principal to the String "anonymousUser". The routes
        // that read a requester used @AuthenticationPrincipal(expression = "username"), which SpEL
        // evaluated against that String unguarded, so every anonymous-allowed read answered 500 with
        // SpelEvaluationException EL1008E. Task 2.88 deleted the expression: @CurrentUser carries no
        // expression, and AuthenticationPrincipalArgumentResolver returns null for a principal that is
        // not an AuthenticatedUser. So that failure class no longer exists.
        //
        // It stays off only because nothing on this chain needs it while every rule here is permitAll
        // and authorization is decided in the controllers. Task 2.133 turns it back on as part of
        // flipping the default to authenticated() -- authorizeHttpRequests cannot distinguish
        // anonymous from unauthenticated without the anonymous token. Whoever does that should know
        // the EL1008E blocker is already gone and check the getName consumers under an anonymous
        // token rather than re-deriving this from the comment that used to stand here.
        http.anonymous(anonymous -> anonymous.disable());
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
