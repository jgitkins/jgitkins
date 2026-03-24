package io.jgitkins.server.application.port.in;

import io.jgitkins.server.application.dto.command.PushEventCommand;

public interface PushEventHandleUseCase {
    void handle(PushEventCommand command);
}
