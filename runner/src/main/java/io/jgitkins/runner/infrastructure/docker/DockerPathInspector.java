package io.jgitkins.runner.infrastructure.docker;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class DockerPathInspector {

    public boolean isReadableDirectory(String path) {
        try {
            Path candidate = Path.of(path);
            return Files.isDirectory(candidate) && Files.isReadable(candidate);
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    public boolean isReadableFile(String path) {
        try {
            Path candidate = Path.of(path);
            return Files.isRegularFile(candidate) && Files.isReadable(candidate);
        } catch (InvalidPathException ex) {
            return false;
        }
    }
}
