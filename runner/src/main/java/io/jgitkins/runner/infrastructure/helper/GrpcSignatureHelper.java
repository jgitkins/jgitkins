package io.jgitkins.runner.infrastructure.helper;

import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import io.jgitkins.server.grpc.JobDispatchServiceGrpc;

public final class GrpcSignatureHelper {

    private static final Metadata.Key<String> HEADER_TIMESTAMP =
            Metadata.Key.of(RunnerHeaderNames.TIMESTAMP, Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> HEADER_NONCE =
            Metadata.Key.of(RunnerHeaderNames.NONCE, Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> HEADER_SIGNATURE =
            Metadata.Key.of(RunnerHeaderNames.SIGNATURE, Metadata.ASCII_STRING_MARSHALLER);

    private GrpcSignatureHelper() {
    }

    public static JobDispatchServiceGrpc.JobDispatchServiceBlockingStub attach(
            JobDispatchServiceGrpc.JobDispatchServiceBlockingStub stub,
            String runnerToken,
            String methodName
    ) {
        RunnerRequestSignature signature = RunnerRequestSigner.forGrpc(runnerToken, methodName);
        Metadata metadata = new Metadata();
        metadata.put(HEADER_TIMESTAMP, signature.timestamp());
        metadata.put(HEADER_NONCE, signature.nonce());
        metadata.put(HEADER_SIGNATURE, signature.signature());
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }
}
