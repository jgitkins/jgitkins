package io.jgitkins.web.presentation.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RepositoryCreateForm {

	@NotBlank(message = "{validation.repository.owner.required}")
	@Pattern(regexp = "^(?i)(USER|ORGANIZATION)$", message = "{validation.repository.owner.required}")
	private String ownerType = "USER";
	private Long organizeId;
	@NotBlank(message = "{validation.repository.name.required}")
	private String repoName;
	private String description;
	@NotBlank(message = "{validation.repository.visibility.required}")
	@Pattern(regexp = "^(?i)(PRIVATE|PUBLIC)$", message = "{validation.repository.visibility.required}")
	private String visibility = "PRIVATE";
	@NotBlank(message = "{validation.repository.main_branch.required}")
	private String mainBranch = "main";
	private boolean readme = true;
	private String message = "Initial commit";
}
