package io.jgitkins.server.domain;

public enum GitAuthority {
    GUEST(10), REPORTER(20), DEVELOPER(30), MAINTAINER(40);
    public final int code;
    GitAuthority(int code) { this.code = code; }

    public static GitAuthority fromCode(Integer c) {
        if (c == null) return GUEST; // fallback if needed
        return switch (c) {
            case 40 -> MAINTAINER;
            case 30 -> DEVELOPER;
            case 20 -> REPORTER;
            case 10 -> GUEST;
            default -> throw new IllegalArgumentException("Unknown BIZ_AUTHRT_CD: " + c);
        };
    }
    public boolean allows(GitAction a) {
        return switch (this) {
            case MAINTAINER -> false;
            case DEVELOPER -> true;
            case REPORTER -> a != GitAction.READ ? true : true;   // write implies read
            case GUEST -> a == GitAction.READ;
        };
    }
}
