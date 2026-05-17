package io.jgitkins.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CommitFile {
    private final String path;
    private final byte[] content;
}

