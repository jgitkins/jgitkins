package io.jgitkins.server.infrastructure.config.git;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.transport.resolver.FileResolver;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
@Slf4j
public class GitHttpConfig {

    private final File repoRoot; // 저장소 루트 경로
    private final FetchEventUploadPackFactory fetchEventUploadPackFactory;
    private final PushEventReceivePackFactory pushEventReceivePackFactory;

    public GitHttpConfig(FetchEventUploadPackFactory fetchEventUploadPackFactory,
                         PushEventReceivePackFactory pushEventReceivePackFactory,
                         @Value("${jgitkins.server.runtime.volume:${user.home}}") String runtimeVolume) {
        this.fetchEventUploadPackFactory = fetchEventUploadPackFactory;
        this.pushEventReceivePackFactory = pushEventReceivePackFactory;
        this.repoRoot = new File(runtimeVolume);
    }

    // Git Servlet Configuration
    @Bean
    public ServletRegistrationBean<GitServlet> gitServlet() {
        GitServlet servlet = new GitServlet();

        RepositoryResolver<HttpServletRequest> resolver = new FileResolver<>(repoRoot, true); // false → exportAll=false
        servlet.setRepositoryResolver(resolver);

        // Fetch Event
        servlet.setUploadPackFactory(fetchEventUploadPackFactory);
        // Push Event
        servlet.setReceivePackFactory(pushEventReceivePackFactory);

        return new ServletRegistrationBean<>(servlet, "/git/*");
    }
}
