package io.jgitkins.runner.infrastructure.helper;

public record RunnerRequestSignature(String timestamp, String nonce, String signature) {
}
