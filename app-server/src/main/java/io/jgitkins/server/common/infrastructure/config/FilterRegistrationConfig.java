package io.jgitkins.server.common.infrastructure.config;

import io.jgitkins.server.common.infrastructure.config.filter.GitSmartHttpCanonicalRedirectFilter;
import io.jgitkins.server.common.infrastructure.config.filter.HttpLogFilter;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterRegistrationConfig {

    @Bean
    public HttpLogFilter httpLogFilter() {
        return new HttpLogFilter();
    }

    @Bean
    public FilterRegistrationBean<HttpLogFilter> httpLogFilterRegistration(HttpLogFilter httpLogFilter) {
        FilterRegistrationBean<HttpLogFilter> filterRegistry = new FilterRegistrationBean<>();
        filterRegistry.setFilter(httpLogFilter);
        filterRegistry.addUrlPatterns("/*");
        filterRegistry.setOrder(-200);
        return filterRegistry;
    }

    @Bean
    public GitSmartHttpCanonicalRedirectFilter gitSmartHttpCanonicalRedirectFilter() {
        return new GitSmartHttpCanonicalRedirectFilter();
    }

    @Bean
    public FilterRegistrationBean<GitSmartHttpCanonicalRedirectFilter> gitSmartHttpCanonicalRedirectFilterRegistration(
            GitSmartHttpCanonicalRedirectFilter gitSmartHttpCanonicalRedirectFilter
    ) {
        FilterRegistrationBean<GitSmartHttpCanonicalRedirectFilter> filterRegistry = new FilterRegistrationBean<>();
        filterRegistry.setFilter(gitSmartHttpCanonicalRedirectFilter);
        filterRegistry.addUrlPatterns("/*");
        filterRegistry.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        filterRegistry.setOrder(-150);
        return filterRegistry;
    }
}
