package io.jgitkins.server.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class CommitHistory {
    private String id;
    private String hash;
    private String shortHash;
    private String authorName;
    private String authorEmail;
    private String committerName;
    private String committerEmail;
    private String message;
    private String shortMessage;
    private String fullMessage;
    private LocalDateTime committedAt;
    private LocalDateTime commitTime;
    private String parentId;
}
