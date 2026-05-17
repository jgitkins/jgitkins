package io.jgitkins.web.infrastructure.config;

import io.jgitkins.web.application.port.out.AppSessionTokenPort;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;

@Slf4j
@Configuration
public class ApiClientConfig {

	@Bean
	public RestClient jgitkinsRestClient(JgitkinsServerProperties properties,
										 RestClient.Builder builder,
										 AppSessionTokenPort tokenPort) {
		return builder
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(new JdkClientHttpRequestFactory())
				.requestInterceptor((request, body, execution) -> {
					String token = tokenPort.getCurrentSessionToken();
					if (token != null && !token.isBlank()) {
						request.getHeaders().setBearerAuth(token);
					}
					return execution.execute(request, body);
				})
				.requestInterceptor((request, body, execution) -> {
					long startTime = System.currentTimeMillis();
					ClientHttpResponse response = null;
					String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
					String uri = request.getURI().toString();
					log.debug("[CLIENT] [REQUEST-SENT] [METHOD {}] [URI {}]", method, uri);
					try {
						response = execution.execute(request, body);
						return response;
					} catch (IOException ex) {
						long durationMs = System.currentTimeMillis() - startTime;
						log.warn("[CLIENT] [RESPONSE-RECEIVED] [STATUS ERROR] [METHOD {}] [URI {}] [DURATION_MS {}] [EX {}]",
								method, uri, durationMs, ex.getClass().getSimpleName());
						throw ex;
					} finally {
						long durationMs = System.currentTimeMillis() - startTime;
						if (response != null) {
							try {
								log.debug("[CLIENT] [RESPONSE-RECEIVED] [STATUS {}] [METHOD {}] [URI {}] [DURATION_MS {}]",
										response.getStatusCode(), method, uri, durationMs);
							} catch (IOException ex) {
								log.debug("[CLIENT] [RESPONSE-RECEIVED] [STATUS UNKNOWN] [METHOD {}] [URI {}] [DURATION_MS {}]",
										method, uri, durationMs);
							}
						}
					}
				})
				.build();
	}
}
