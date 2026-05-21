package io.jgitkins.server.execution.application.port.in;

import io.jgitkins.server.shared.application.command.PushEventCommand;

public interface PushEventHandleUseCase {
    void handle(PushEventCommand command);
}
