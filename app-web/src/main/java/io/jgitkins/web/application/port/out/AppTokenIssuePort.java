package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.dto.OAuthLoginRequest;
import io.jgitkins.web.application.dto.ServerOAuthLoginResult;

public interface AppTokenIssuePort {

	ServerOAuthLoginResult issueOAuthLoginToken(OAuthLoginRequest request);
}
