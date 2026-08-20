package io.jgitkins.server.execution.adapter.in.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JobDispatchProtoSourceSynchronizationTest {
    @Test
    void serverAndRunnerProtoSourcesRemainByteIdentical() throws Exception {
        Path server = Path.of("src/main/proto/job_dispatch.proto");
        Path runner = Path.of("../app-runner/src/main/proto/job-dispatch.proto");
        assertThat(Files.readAllBytes(server)).isEqualTo(Files.readAllBytes(runner));
    }
}
