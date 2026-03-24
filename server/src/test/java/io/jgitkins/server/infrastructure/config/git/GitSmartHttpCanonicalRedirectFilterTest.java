package io.jgitkins.server.infrastructure.config.git;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.infrastructure.config.filter.GitSmartHttpCanonicalRedirectFilter;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class GitSmartHttpCanonicalRedirectFilterTest {

    private GitSmartHttpCanonicalRedirectFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GitSmartHttpCanonicalRedirectFilter();
    }

    @Test
    void redirectsRootGitPathToGitPrefix_withQueryPreserved() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/hrk11mmmm/repo.git/info/refs");
        request.setQueryString("service=git-receive-pack");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(308);
        assertThat(response.getHeader("Location"))
                .isEqualTo("/git/hrk11mmmm/repo.git/info/refs?service=git-receive-pack");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void passesThroughWhenPathAlreadyHasGitPrefix() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/git/hrk11mmmm/repo.git/info/refs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
