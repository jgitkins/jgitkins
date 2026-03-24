package io.jgitkins.server.infrastructure.adapter.git;

import io.jgitkins.server.application.dto.pipeline.PipelineConfig;
import io.jgitkins.server.application.dto.pipeline.PipelineRule;
import io.jgitkins.server.application.port.out.PipelineConfigPort;
import io.jgitkins.server.infrastructure.common.error.InfrastructureErrorCode;
import io.jgitkins.server.infrastructure.exception.InfrastructureException;
import io.jgitkins.server.infrastructure.support.RepositoryResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class PipelineConfigGitAdapter implements PipelineConfigPort {

    private static final String CONFIG_PATH = ".jgitkins/ci.yml";

    private final RepositoryResolver repositoryResolver;
    private final Yaml yaml = new Yaml();

    public PipelineConfigGitAdapter(RepositoryResolver repositoryResolver) {
        this.repositoryResolver = repositoryResolver;
    }

    @Override
    public PipelineConfig read(String namespace, String repoName, String commitHash) {
        try (Repository repository = repositoryResolver.openBareRepository(namespace, repoName)) {
            RevTree tree = resolveCommitTree(repository, commitHash);
            String yamlText = readConfig(repository, tree);
            if (yamlText == null || yamlText.isBlank()) {
                return emptyConfig();
            }

            Object loaded = yaml.load(yamlText);
            if (!(loaded instanceof Map<?, ?> root)) {
                return emptyConfig();
            }

            return toPipelineConfig(root);
        } catch (InfrastructureException e) {
            throw e;
        } catch (Exception e) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.FILE_LOAD_FAILED,
                    "Failed to load pipeline config from " + CONFIG_PATH,
                    e);
        }
    }

    private RevTree resolveCommitTree(Repository repository, String commitHash) throws IOException {
        ObjectId commitId = repository.resolve(commitHash);
        if (commitId == null) {
            throw new InfrastructureException(
                    InfrastructureErrorCode.FILE_LOAD_FAILED,
                    "Commit not found for pipeline config load: " + commitHash);
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(commitId);
            return commit.getTree();
        }
    }

    private String readConfig(Repository repository, RevTree tree) throws IOException {
        try (TreeWalk treeWalk = TreeWalk.forPath(repository, CONFIG_PATH, tree)) {
            if (treeWalk == null) {
                return null;
            }

            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
            return new String(loader.getBytes(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    private PipelineConfig toPipelineConfig(Map<?, ?> root) {
        Object onObject = findOnSection(root);
        if (!(onObject instanceof Map<?, ?> on)) {
            return emptyConfig();
        }

        Object pushObject = on.get("push");
        if (!(pushObject instanceof Map<?, ?> push)) {
            return emptyConfig();
        }

        Object rulesObject = push.get("rules");
        if (!(rulesObject instanceof List<?> rules)) {
            return emptyConfig();
        }

        List<PipelineRule> mappedRules = rules.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(rule -> toRule((Map<?, ?>) rule))
                .filter(rule -> !rule.getBranches().isEmpty() && rule.getFile() != null && !rule.getFile().isBlank())
                .toList();

        return new PipelineConfig(mappedRules);
    }

    private Object findOnSection(Map<?, ?> root) {
        if (root.containsKey("on")) {
            return root.get("on");
        }

        // SnakeYAML can parse bare "on" as boolean true depending on YAML schema.
        if (root.containsKey(Boolean.TRUE)) {
            return root.get(Boolean.TRUE);
        }

        return null;
    }

    private PipelineRule toRule(Map<?, ?> rawRule) {
        Object branchesObject = rawRule.get("branches");
        Object fileObject = rawRule.get("file");

        List<String> branches = branchesObject instanceof List<?> branchList
                ? branchList.stream().map(String::valueOf).toList()
                : List.of();

        String file = fileObject == null ? null : String.valueOf(fileObject);
        return new PipelineRule(branches, file);
    }

    private PipelineConfig emptyConfig() {
        return new PipelineConfig(List.of());
    }
}
