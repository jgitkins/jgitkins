package io.jgitkins.server.domain.model.vo;

public final class InitialCommitOptions {
    private final boolean initializeReadme;
    private final String commitMessage;
    private final String authorName;
    private final String authorEmail;

    private InitialCommitOptions(boolean initializeReadme, String commitMessage, String authorName, String authorEmail) {
        this.initializeReadme = initializeReadme;
        this.commitMessage = commitMessage;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
    }

    public static InitialCommitOptions of(boolean initializeReadme,
                                          String commitMessage,
                                          String authorName,
                                          String authorEmail) {
        String normalizedMessage = normalize(commitMessage);
        if (initializeReadme && normalizedMessage == null) {
            throw new IllegalArgumentException("Initial commit message is required when README initialization is enabled");
        }
        if (!initializeReadme && normalizedMessage != null) {
            throw new IllegalArgumentException("Initial commit message must be omitted when README initialization is disabled");
        }
        return new InitialCommitOptions(initializeReadme,
                                        normalizedMessage,
                                        normalize(authorName),
                                        normalize(authorEmail));
    }

    public boolean requiresInitialContent() {
        return initializeReadme;
    }

    public String commitMessage() {
        return commitMessage;
    }

    public String authorName() {
        return authorName;
    }

    public String authorEmail() {
        return authorEmail;
    }

    private static String normalize(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
