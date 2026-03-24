package io.jgitkins.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileIndexEntry {
    private final String name;
    private final String path;
    private final String type;
}
