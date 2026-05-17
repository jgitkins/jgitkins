package io.jgitkins.server.change.review.domain.model;

import io.jgitkins.server.domain.model.vo.CommitHash;

public record TargetDrift(
        boolean drifted,
        CommitHash previousTargetHead,
        CommitHash currentTargetHead
) {

    public TargetDrift {
        if (drifted) {
            if (previousTargetHead == null || currentTargetHead == null) {
                throw new IllegalArgumentException("Target drift requires previous and current target heads");
            }
            if (previousTargetHead.equals(currentTargetHead)) {
                throw new IllegalArgumentException("Target drift requires different target heads");
            }
        }
    }

    public static TargetDrift none() {
        return new TargetDrift(false, null, null);
    }

    public static TargetDrift detected(CommitHash previousTargetHead, CommitHash currentTargetHead) {
        return new TargetDrift(true, previousTargetHead, currentTargetHead);
    }
}
