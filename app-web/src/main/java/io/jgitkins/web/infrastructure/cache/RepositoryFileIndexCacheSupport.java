package io.jgitkins.web.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jgitkins.web.application.contract.RepositoryFileIndexEntry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryFileIndexCacheSupport {

    private static final TypeReference<List<RepositoryFileIndexEntry>> FILE_LIST_TYPE = new TypeReference<>() { };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public Optional<List<RepositoryFileIndexEntry>> get(String namespace, String repoName, String branch, String headCommit) {
        String key = buildKey(namespace, repoName, branch, headCommit);
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(raw)) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(raw, FILE_LIST_TYPE));
        } catch (Exception ex) {
            log.debug("Repository file index cache lookup failed. key={}", key, ex);
            return Optional.empty();
        }
    }

    public void put(String namespace,
                    String repoName,
                    String branch,
                    String headCommit,
                    List<RepositoryFileIndexEntry> files,
                    Duration ttl) {
        String key = buildKey(namespace, repoName, branch, headCommit);
        try {
            String raw = objectMapper.writeValueAsString(files == null ? List.of() : files);
            redisTemplate.opsForValue().set(key, raw, ttl);
        } catch (Exception ex) {
            log.debug("Repository file index cache write failed. key={}", key, ex);
        }
    }

    private String buildKey(String namespace, String repoName, String branch, String headCommit) {
        String commitPart = StringUtils.hasText(headCommit) ? headCommit : "no-head";
        return "jgitkins:web:repo-file-index:%s:%s:%s:%s".formatted(namespace, repoName, branch, commitPart);
    }
}
