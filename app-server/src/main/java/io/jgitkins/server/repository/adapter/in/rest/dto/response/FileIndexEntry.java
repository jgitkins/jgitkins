package io.jgitkins.server.repository.adapter.in.rest.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileIndexEntry {
    private final String name;
    private final String path;
    private final String type;
}
