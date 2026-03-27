package io.jgitkins.runner.infrastructure.helper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class RunnerRequestSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private RunnerRequestSigner() {
    }

    public static RunnerRequestSignature forHttp(String token, String method, String path) {
        return sign(token, canonical(method, path));
    }

    public static RunnerRequestSignature forGrpc(String token, String path) {
        return sign(token, canonical("GRPC", path));
    }

    private static RunnerRequestSignature sign(String token, String payload) {
        String timestamp = Long.toString(Instant.now().toEpochMilli());
        String nonce = UUID.randomUUID().toString();
        String data = payload + "\n" + timestamp + "\n" + nonce;
        String signature = hmac(token, data);
        return new RunnerRequestSignature(timestamp, nonce, signature);
    }

    private static String canonical(String method, String target) {
        return method.toUpperCase() + "\n" + target;
    }

    private static String hmac(String key, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute request signature.", ex);
        }
    }
}
