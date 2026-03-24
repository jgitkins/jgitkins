package io.jgitkins.server.application.dto;

/**
 * Job dispatch routing scope.
 * 메시지를 어떤 Runner 그룹으로 보낼지 결정한다.
 */
public enum JobDispatchScope {
    GLOBAL,
    ORGANIZE,
    REPOSITORY
}
