package io.jgitkins.server.application.port.out;

public interface RuntimeConfigPort {

    String serviceHost();

    String restScheme();

    Integer restPort();

    String restBasePath();

    Integer grpcPort();

    Long pollIntervalMs();

    Long busyWaitIntervalMs();
}
