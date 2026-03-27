package io.jgitkins.runner.application.port.out;

import java.nio.file.Path;

public interface RepositorySyncPort {

    Path fetchRepository(String repoUrl, String taskCd, String repoName, String ref) throws Exception;
}
