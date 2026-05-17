package io.jgitkins.web.application.dto;

public record RepositoryFileIndexEntry(
        String name,
        String path,
        String type
) {
}
