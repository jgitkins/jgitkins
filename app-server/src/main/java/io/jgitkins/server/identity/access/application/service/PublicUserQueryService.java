package io.jgitkins.server.identity.access.application.service;

import io.jgitkins.server.identity.access.application.dto.result.UserSummary;
import io.jgitkins.server.identity.access.application.mapper.UserApplicationMapper;
import io.jgitkins.server.identity.access.application.port.in.PublicUserQueryUseCase;
import io.jgitkins.server.identity.access.application.port.out.UserPersistencePort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicUserQueryService implements PublicUserQueryUseCase {

    private final UserPersistencePort userPort;
    private final UserApplicationMapper userApplicationMapper;

    @Override
    public List<UserSummary> getUsers() {
        return userPort.findAll()
                .stream()
                .map(userApplicationMapper::toUserSummary)
                .toList();
    }
}
