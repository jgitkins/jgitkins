package io.jgitkins.server.infrastructure.config.git;

import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class GitSmartHttpEventParser {

    private static final Pattern ROOT_GIT_PATH = Pattern.compile("^/[^/]+/[^/]+\\.git(?:/.*)?$");
    private static final Pattern PREFIX_GIT_PATH = Pattern.compile("^/git/[^/]+/[^/]+\\.git(?:/.*)?$");
    private static final Pattern LEGACY_GIT_PATH = Pattern.compile("^/git/(user|users|organize|organization|org)/[^/]+/[^/]+\\.git(?:/.*)?$");

    public GitSmartHttpEvent parse(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path == null || path.isBlank()) {
            return null;
        }

        boolean hasGitPrefix = path.startsWith("/git/");
        if (hasGitPrefix) {
            if (!PREFIX_GIT_PATH.matcher(path).matches() && !LEGACY_GIT_PATH.matcher(path).matches()) {
                return null;
            }
        } else if (!ROOT_GIT_PATH.matcher(path).matches()) {
            return null;
        }
        String trimmed = hasGitPrefix ? path.substring("/git".length()) : path;
        trimmed = trimLeadingSlashes(trimmed);
        if (trimmed.isBlank()) {
            return null;
        }
        String[] parts = trimmed.split("/");
        if (parts.length < 2) {
            return null;
        }

        String ownerName;
        String repoSegment;
        if (hasGitPrefix) {
            if (parts.length >= 3 && isOwnerTypePrefix(parts[0])) {
                ownerName = parts[1];
                repoSegment = parts[2];
            } else {
                ownerName = parts[0];
                repoSegment = parts[1];
            }
        } else {
            ownerName = parts[0];
            repoSegment = parts[1];
        }

        String repositoryName = repoSegment.endsWith(".git")
                ? repoSegment.substring(0, repoSegment.length() - 4)
                : repoSegment;
        if (ownerName.isBlank() || repositoryName.isBlank()) {
            return null;
        }
        return new GitSmartHttpEvent(ownerName, repositoryName);
    }

    private String trimLeadingSlashes(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("^/+", "");
    }

    private boolean isOwnerTypePrefix(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.equals("user")
                || normalized.equals("users")
                || normalized.equals("organize")
                || normalized.equals("organization")
                || normalized.equals("org");
    }
}
