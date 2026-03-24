package io.jgitkins.server.application.event;

import io.jgitkins.server.application.exception.ApplicationException;
import io.jgitkins.server.application.dto.CommitFile;
import io.jgitkins.server.application.factory.CommitFileFactory;
import io.jgitkins.server.application.port.out.*;
import io.jgitkins.server.application.support.RepositoryNamespaceResolver;
import io.jgitkins.server.domain.Branch;
import io.jgitkins.server.domain.aggregate.Repository;
import io.jgitkins.server.domain.event.RepositoryProvisionedEvent;
import io.jgitkins.server.domain.model.vo.RepositoryName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryProvisionedEventListener {

    private final CommitFileFactory commitFileFactory;

    private final RepositoryPersistencePort repositoryPort;
    private final BranchPersistencePort branchPort;
    private final RepositoryNamespaceResolver repositoryNamespaceResolver;

    private final CommitGitPort commitGitPort;
    private final RepositoryGitPort repositoryGitPort;

    // post progressing
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // contain require_new
    @Transactional(propagation = REQUIRES_NEW)
    public void onRepositoryProvisioned(RepositoryProvisionedEvent event) {
        RepositoryName repositoryName = event.getName();
        String repoNameValue = repositoryName.getValue();
        String branchName = event.getDefaultBranch().getValue();
        String namespace = repositoryNamespaceResolver.resolve(event.getOwnerType(), event.getOwnerId());

        Repository repository = loadRepository(event, repositoryName, repoNameValue);

        // 기본 브랜치 엔트리를 미리 생성해 애플리케이션 상태를 일관되게 유지한다.
        Branch defaultBranch = Branch.create(repository.getId().getValue(),
                branchName,
                false,
                true,
                true);
        branchPort.save(defaultBranch);

        if (event.getInitialCommitOptions() != null && event.getInitialCommitOptions().requiresInitialContent()) {
            List<CommitFile> files = commitFileFactory.prepareInitialFile(repoNameValue);
            commitGitPort.commit(namespace,
                    repoNameValue,
                    branchName,
                    event.getInitialCommitOptions().commitMessage(),
                    event.getInitialCommitOptions().authorName(),
                    event.getInitialCommitOptions().authorEmail(),
                    files);
            repositoryGitPort.updateHeadReference(namespace, repoNameValue, branchName);
            log.info("repository has initialized with readme");

            repositoryPort.update(repository.markInit(LocalDateTime.now()));
        }
    }

    private Repository loadRepository(RepositoryProvisionedEvent event,
            RepositoryName repositoryName,
            String repoNameValue) {
        return repositoryPort.findByOwnerAndName(event.getOwnerType(), event.getOwnerId(), repositoryName)
                .orElseThrow(() -> new ApplicationException(
                        io.jgitkins.server.application.common.error.ApplicationErrorCode.REPOSITORY_NOT_FOUND,
                        "Repository not found for event: " + repoNameValue));
    }

}
