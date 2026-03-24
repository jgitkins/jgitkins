package io.jgitkins.server.application.dto.pipeline;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PipelineRule {

    private final List<String> branches;
    private final String file;

    public boolean matches(String branchName) {
        if (branches == null || branches.isEmpty()) {
            return false;
        }

        for (String candidate : branches) {
            if (candidate.equals(branchName)) {
                return true;
            }

            if (candidate.endsWith("/*")) {
                String prefix = candidate.substring(0, candidate.length() - 1);
                if (branchName.startsWith(prefix)) {
                    return true;
                }
            }
        }

        return false;
    }
}
