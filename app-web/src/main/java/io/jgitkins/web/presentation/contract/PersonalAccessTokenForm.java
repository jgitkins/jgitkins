package io.jgitkins.web.presentation.contract;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonalAccessTokenForm {
	@NotBlank(message = "{validation.pat.fields.required}")
	private String name;
	@NotBlank(message = "{validation.pat.fields.required}")
	private String description;
	@NotBlank(message = "{validation.pat.fields.required}")
	private String expiration;
}
