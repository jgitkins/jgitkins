package io.jgitkins.web.application.contract;

public record RepositoryFileIndexEntry(
        String name,
        String path,
        String type
) {
}
