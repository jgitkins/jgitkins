package io.jgitkins.web.presentation.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizeCreateForm {

	@NotBlank(message = "{validation.organize.name.required}")
	@Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "{validation.organize.name.pattern}")
	private String name;
	private String description;
}
