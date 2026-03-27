package io.jgitkins.runner.infrastructure.http;

import io.jgitkins.runner.application.exception.RunnerRegistrationException;
import io.jgitkins.runner.infrastructure.helper.ApiErrorMessageExtractor;
import io.jgitkins.runner.infrastructure.helper.EndpointPaths;
import io.jgitkins.runner.infrastructure.helper.RunnerActivationEndpointHelper;
import io.jgitkins.runner.infrastructure.helper.RunnerHeaderNames;
import io.jgitkins.runner.infrastructure.helper.RunnerRequestSignature;
import io.jgitkins.runner.infrastructure.helper.RunnerRequestSigner;
import io.jgitkins.runner.presentation.common.ApiResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RunnerActivateClient {

    private final ApiErrorMessageExtractor errorMessageExtractor;
    private final RestClient restClient = RestClient.create();

    public ActivationResult activateRunner(String runnerToken, String baseUrl) {
        try {
            String endpoint = RunnerActivationEndpointHelper.build(baseUrl);
            RunnerRequestSignature signature = RunnerRequestSigner.forHttp(runnerToken, "POST", EndpointPaths.Rest.ACTIVATE);
            ApiResponse<ActivationResult> body = restClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.add(RunnerHeaderNames.TIMESTAMP, signature.timestamp());
                        headers.add(RunnerHeaderNames.NONCE, signature.nonce());
                        headers.add(RunnerHeaderNames.SIGNATURE, signature.signature());
                    })
                    .body(new RunnerActivationRequest(runnerToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            return Optional.ofNullable(body)
                    .map(ApiResponse::getData)
                    .orElseThrow(() -> new IllegalStateException("Activation result is missing"));
        } catch (RestClientResponseException e) {
            int statusCode = e.getStatusCode().value();
            if (statusCode == 409) {
                log.warn("Runner registration failed due to unusable token.");
                throw new RunnerRegistrationException(statusCode, "등록 토큰을 사용할 수 없습니다.", e);
            }
            String message = errorMessageExtractor.extract(e.getResponseBodyAsString())
                    .orElse("Runner registration failed with status " + statusCode);
            log.error(message);
            throw new RunnerRegistrationException(statusCode, message, e);
        }
    }
}
