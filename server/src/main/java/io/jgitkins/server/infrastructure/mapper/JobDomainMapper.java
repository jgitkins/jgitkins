package io.jgitkins.server.infrastructure.mapper;

import io.jgitkins.server.domain.aggregate.Job;
import io.jgitkins.server.domain.model.JobHistory;
import io.jgitkins.server.domain.model.vo.*;
import io.jgitkins.server.infrastructure.persistence.model.DispatchableJobRow;
import io.jgitkins.server.infrastructure.persistence.model.JobEntity;
import io.jgitkins.server.infrastructure.persistence.model.JobHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface JobDomainMapper {

    Logger LOG = LoggerFactory.getLogger(JobDomainMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "repositoryId", expression = "java(job.getRepositoryId().getValue())")
    @Mapping(target = "commitHash", expression = "java(job.getCommitHash().getValue())")
    @Mapping(target = "branchName", expression = "java(job.getBranchName().getValue())")
    @Mapping(target = "triggeredBy", expression = "java(job.getTriggeredBy().getValue())")
    @Mapping(target = "createdAt", source = "createdAt")
    JobEntity toEntity(Job job);

    default Job toDomain(JobEntity entity, List<JobHistory> histories) {
        if (entity == null) {
            return null;
        }
        return Job.reconstruct(
                JobId.of(String.valueOf(entity.getId())),
                RepositoryId.of(entity.getRepositoryId()),
                CommitHash.of(entity.getCommitHash()),
                BranchName.of(entity.getBranchName()),
                UserId.of(entity.getTriggeredBy()),
                entity.getCreatedAt(),
                histories);
    }

    default Job toDomain(DispatchableJobRow row, List<JobHistory> histories) {
        if (row == null) {
            return null;
        }
        return Job.reconstruct(
                JobId.of(String.valueOf(row.jobId())),
                RepositoryId.of(row.repositoryId()),
                CommitHash.of(row.commitHash()),
                BranchName.of(row.branchName()),
                UserId.of(row.triggeredBy()),
                row.jobCreatedAt(),
                histories);
    }

    default JobHistory toHistoryDomain(JobHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        // DB에 없는 필드(seqNo, createdBy)는 임시값 사용
        return JobHistory.reconstruct(
                JobHistoryId.of(String.valueOf(entity.getId())),
                JobId.of(String.valueOf(entity.getJobId())),
                SequenceNumber.of(1), // TODO: DB 필드 추가 필요
                entity.getRunnerId() != null ? RunnerId.of(String.valueOf(entity.getRunnerId())) : null,
                JobStatus.valueOf(entity.getStatus()),
                SystemUser.SYSTEM, // TODO: DB 필드 추가 필요
                entity.getCreatedAt());
    }

    default JobHistoryEntity toHistoryEntity(JobHistory history, Long jobId) {
        if (history == null) {
            return null;
        }
        JobHistoryEntity entity = new JobHistoryEntity();
        entity.setJobId(jobId);
        // Entity에 없는 필드는 세팅 제외
        entity.setStatus(history.getStatus().name());
        entity.setCreatedAt(history.getCreatedAt());
        entity.setRunnerId(convertRunnerId(history.getRunnerId()));
        return entity;
    }

    private Long convertRunnerId(RunnerId runnerId) {
        if (runnerId == null) {
            return null;
        }
        String runnerValue = runnerId.getValue();
        if (runnerValue == null || runnerValue.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(runnerValue);
        } catch (NumberFormatException ex) {
            LOG.warn("RunnerId [{}] is not numeric. runner_id will be stored as null.", runnerValue);
            return null;
        }
    }
}
