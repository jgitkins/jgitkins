package io.jgitkins.server.application.common;


// TODO: 운영관점에서 변경될 소지 있는부분 yml or DB 설정으로 옮기기
public final class GitConstants {
    private GitConstants() {}

    public static final String REFS_HEADS_PREFIX = "refs/heads/";
    public static final String REFS_REMOTES_PREFIX = "refs/remotes/";
    public static final String REFS_TAGS_PREFIX = "refs/tags/";
}
