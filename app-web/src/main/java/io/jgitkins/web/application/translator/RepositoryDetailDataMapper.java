package io.jgitkins.web.application.translator;

import io.jgitkins.web.application.contract.BranchSummary;
import io.jgitkins.web.application.contract.RepositoryDetailData;
import io.jgitkins.web.application.contract.RepositoryFileEntry;
import io.jgitkins.web.application.contract.RepositorySummary;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
public interface RepositoryDetailDataMapper {

	@Mapping(target = "repository", source = "repository")
	@Mapping(target = "branches", source = "branches")
	@Mapping(target = "files", source = "files")
	@Mapping(target = "namespace", source = "namespace")
	@Mapping(target = "ownerSlug", source = "ownerSlug")
	@Mapping(target = "repoName", source = "repoName")
	@Mapping(target = "selectedBranch", expression = "java(resolveSelectedBranch(selectedBranch))")
	@Mapping(target = "role", source = "role")
	@Mapping(target = "writable", source = "writable")
	@Mapping(target = "errorMessage", expression = "java((String) null)")
	RepositoryDetailData toDetail(RepositorySummary repository,
								 List<BranchSummary> branches,
								 List<RepositoryFileEntry> files,
								 String namespace,
								 String ownerSlug,
								 String repoName,
								 String selectedBranch,
								 String role,
								 boolean writable);

	default String resolveSelectedBranch(String selectedBranch) {
		return StringUtils.hasText(selectedBranch) ? selectedBranch : "main";
	}
}
