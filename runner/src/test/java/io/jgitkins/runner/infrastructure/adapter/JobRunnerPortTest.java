package io.jgitkins.runner.infrastructure.adapter;

import com.github.dockerjava.api.DockerClient;
import io.jgitkins.runner.application.dto.JobRunContext;
import io.jgitkins.runner.application.port.out.JobRunnerPort;
import io.jgitkins.runner.application.port.out.RepositorySyncPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Test double for {@link JobRunnerPort} that bypasses Docker interactions during integration tests.
 */
@Component
@Primary
@Profile("test")
//@SpringBootTest
public class JobRunnerPortTest {

    @Autowired
    private RepositorySyncPort repositorySyncPort;

    @Autowired
    private DockerClient dockerClient;
    private JobRunnerPort containerRunnerPort;

//    @BeforeEach
//    void setUp() {
//        repositorySyncPort = new GitRepositorySyncAdapter();
//        DefaultDockerClientConfig config = DefaultDockerClientConfig
//                .createDefaultConfigBuilder()
//                .withDockerHost("unix:///var/run/docker.sock")
//                .build();
//
//        DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
//                .dockerHost(config.getDockerHost())
//                .sslConfig(config.getSSLConfig())
//                .build();
//
//        dockerClient = DockerClientImpl.getInstance(config, httpClient);
//
//        containerRunnerPort = new DockerRunnerAdapter(dockerClient);
//    }

    @Test
    void test_run_successful() {

        String image = "jenkins/jenkinsfile-runner";
        String pluginConfigPath = "/Users/hwiryungkim/TMP/runner/plugins.txt";
        Path workspace = null;
        try {
            String cloneUrl = "http://localhost:8084/git/demo-org/ccc.git";
            String taskCd = "demo-org";
            String repoName = "ccc";
            String ref = "378e2aff9a5bddf5f2db16d1e9182cfdf230ac29";
            workspace = repositorySyncPort.fetchRepository(cloneUrl, taskCd, repoName, ref);
            JobRunContext context = JobRunContext.builder()
                    .workspacePath(workspace.toAbsolutePath().toString())
                    .runnerImageName(image)
                    .pluginPath(pluginConfigPath)
                    .build();
            containerRunnerPort.run(context);
        } catch (Exception e) {
//            System.out.println("error : , e.getMessage(), e)k;

            System.out.println("error: " + e);
        }


    }

}
