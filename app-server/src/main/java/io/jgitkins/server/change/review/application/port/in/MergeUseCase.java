package io.jgitkins.server.change.review.application.port.in;

import io.jgitkins.server.change.review.application.contract.command.MergeRequest;
import io.jgitkins.server.change.review.application.contract.result.MergeResult;
import java.io.IOException;

public interface MergeUseCase {

    /**
     * @param requesterUserId supplied by the inbound adapter, NOT read off {@code request}.
     *     {@code MergeRequest} is bound from the HTTP body, so an actor field on it would let the
     *     caller name themselves and turn the authorization check into a formality.
     */
    MergeResult performMerge(String namespace, String repoName, MergeRequest request,
            Long requesterUserId) throws IOException;
}
