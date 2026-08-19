package io.jgitkins.runner.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionDispatchProtocolCompatibilityTest {
    @Test
    void runnerGeneratedDescriptor_matchesDispatchContract() throws Exception {
        Class<?> service = Class.forName("io.jgitkins.server.grpc.JobDispatchServiceGrpc");
        Object descriptor = service.getMethod("getServiceDescriptor").invoke(null);
        Method getName = descriptor.getClass().getMethod("getName");
        Method getMethods = descriptor.getClass().getMethod("getMethods");
        @SuppressWarnings("unchecked")
        List<Object> methods = (List<Object>) getMethods.invoke(descriptor);
        assertThat(getName.invoke(descriptor)).isEqualTo("jgitkins.dispatch.JobDispatchService");
        assertThat(methods.stream().map(method -> {
            try {
                String fullName = (String) method.getClass().getMethod("getFullMethodName").invoke(method);
                return fullName.substring(fullName.lastIndexOf('/') + 1);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
        })).contains("RequestJob", "ReportJobResult");

        Class<?> payload = Class.forName("io.jgitkins.server.grpc.JobPayload");
        Object payloadDescriptor = payload.getMethod("getDescriptor").invoke(null);
        @SuppressWarnings("unchecked")
        List<Object> fields = (List<Object>) payloadDescriptor.getClass().getMethod("getFields").invoke(payloadDescriptor);
        assertThat(fields).extracting(
                field -> invoke(field, "getName"),
                field -> invoke(field, "getNumber"))
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("jobId", 1),
                        org.assertj.core.groups.Tuple.tuple("jobHistoryId", 2),
                        org.assertj.core.groups.Tuple.tuple("runnerId", 3),
                        org.assertj.core.groups.Tuple.tuple("repositoryId", 4),
                        org.assertj.core.groups.Tuple.tuple("organizeId", 5),
                        org.assertj.core.groups.Tuple.tuple("commitHash", 6),
                        org.assertj.core.groups.Tuple.tuple("branchName", 7),
                        org.assertj.core.groups.Tuple.tuple("triggeredBy", 8),
                        org.assertj.core.groups.Tuple.tuple("cloneUrl", 9));
    }

    private static Object invoke(Object target, String method) {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
