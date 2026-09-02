package io.jgitkins.web.application.port.out;

import io.jgitkins.web.application.contract.OAuthLoginRequest;
import io.jgitkins.web.application.contract.ServerOAuthLoginResult;

public interface AppTokenIssuePort {

	ServerOAuthLoginResult issueOAuthLoginToken(OAuthLoginRequest request);
}
