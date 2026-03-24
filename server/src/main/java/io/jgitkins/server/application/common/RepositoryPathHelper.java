package io.jgitkins.server.application.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RepositoryPathHelper {

    public String buildClonePath(String namespace, String repoPath) {
        String namespaceSegment = trimSlashes(namespace);
        String repoSegment = trimSlashes(repoPath);
        if (!repoSegment.endsWith(".git")) {
            repoSegment = repoSegment + ".git";
        }
        return "/" + namespaceSegment + "/" + repoSegment;
    }

    public String buildUserNamespace(String ownerName) {
        return trimSlashes(ownerName);
    }

    public String buildOrganizeNamespace(String ownerName) {
        return trimSlashes(ownerName);
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
