package io.jgitkins.server.infrastructure.support;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class RepositoryResolver {

    private String rootPath;

    public RepositoryResolver(@Value("${jgitkins.server.runtime.volume:${user.home}}") String runtimeVolume) {
        this.rootPath = runtimeVolume;
    }

    public Repository openBareRepository(String namespace, String repoName) throws IOException {
        return openBareRepository(resolveGitDir(namespace, repoName));
    }

    public Repository openBareRepository(File gitDir) throws IOException {
        return new RepositoryBuilder().setBare().setGitDir(gitDir).build();
    }

    public File resolveGitDir(String namespace, String repoName) {
        File repoRoot = new File(rootPath + "/" + namespace);
        String finalRepoName = repoName.endsWith(".git") ? repoName : repoName + ".git";
        return new File(repoRoot, finalRepoName);
    }
}
