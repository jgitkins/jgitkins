package io.jgitkins.server.identity.access.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jgitkins.server.identity.access.application.contract.result.UserQueryResult;
import io.jgitkins.server.identity.access.application.dto.result.UserSummary;
import io.jgitkins.server.identity.access.application.mapper.UserApplicationMapper;
import io.jgitkins.server.identity.access.application.port.out.UserQueryPort;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PublicUserQueryServiceTest {
    @Mock private UserQueryPort userQueryPort;
    private final UserApplicationMapper userApplicationMapper = Mappers.getMapper(UserApplicationMapper.class);
    private PublicUserQueryService publicUserQueryService;

    @BeforeEach
    void setUp() { publicUserQueryService = new PublicUserQueryService(userQueryPort, userApplicationMapper); }

    @Test
    void getUsers_mapsQueryResultsToSummaries() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        when(userQueryPort.findAll()).thenReturn(List.of(new UserQueryResult(1L, "alice", "alice@example.com",
                "Alice", "https://img/alice.png", "ACTIVE", null, createdAt, createdAt)));

        List<UserSummary> result = publicUserQueryService.getUsers();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("alice", result.get(0).username());
        assertEquals("Alice", result.get(0).displayName());
        assertEquals(createdAt, result.get(0).createdAt());
        verify(userQueryPort).findAll();
    }
}
