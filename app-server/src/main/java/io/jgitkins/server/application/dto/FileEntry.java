package io.jgitkins.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class FileEntry {

    private String id;
    private String name;
    private String path;
    private String type; // e.g. "FILE", "DIRECTORY"
    private String mode;
    private Long size;
    private boolean isDirectory;
}
