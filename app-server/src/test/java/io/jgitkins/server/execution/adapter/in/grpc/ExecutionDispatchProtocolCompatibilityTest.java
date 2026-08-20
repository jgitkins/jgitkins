package io.jgitkins.server.execution.adapter.in.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.jgitkins.server.grpc.JobDispatchServiceGrpc;
import io.jgitkins.server.grpc.JobPayload;
import org.junit.jupiter.api.Test;

class ExecutionDispatchProtocolCompatibilityTest {
    @Test void descriptor_preservesDispatchServiceAndWireNumbers() {
        var service = JobDispatchServiceGrpc.getServiceDescriptor();
        assertThat(service.getName()).isEqualTo("jgitkins.dispatch.JobDispatchService");
        assertThat(service.getMethods().stream().map(method -> method.getFullMethodName().substring(method.getFullMethodName().lastIndexOf('/') + 1)).toList())
                .contains("RequestJob", "ReportJobResult");
        assertThat(JobPayload.getDescriptor().getFields()).extracting("name", "number")
                .containsExactly(tuple("jobId", 1), tuple("jobHistoryId", 2), tuple("runnerId", 3), tuple("repositoryId", 4), tuple("organizeId", 5), tuple("commitHash", 6), tuple("branchName", 7), tuple("triggeredBy", 8), tuple("cloneUrl", 9));
    }
    private static org.assertj.core.groups.Tuple tuple(String name, int number) { return org.assertj.core.api.Assertions.tuple(name, number); }
}
