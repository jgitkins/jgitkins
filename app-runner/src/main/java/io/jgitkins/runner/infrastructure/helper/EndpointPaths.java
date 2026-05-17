package io.jgitkins.runner.infrastructure.helper;

public final class EndpointPaths {

    private EndpointPaths() {
    }

    public static final class Rest {
        public static final String ACTIVATE = "/api/runners/activate";

        private Rest() {
        }
    }

    public static final class Grpc {
        public static final String JOB_REQUEST = "JobDispatchService/requestJob";
        public static final String JOB_RESULT = "JobDispatchService/reportJobResult";

        private Grpc() {
        }
    }
}
