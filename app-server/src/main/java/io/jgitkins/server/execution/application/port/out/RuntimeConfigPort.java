package io.jgitkins.server.execution.application.port.out;

public interface RuntimeConfigPort {

    String serviceHost();

    String restScheme();

    Integer restPort();

    String restBasePath();

    Integer grpcPort();

    Long pollIntervalMs();

    Long busyWaitIntervalMs();
}
