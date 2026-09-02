package io.jgitkins.web.presentation.contract;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormValidationTest {

	private Validator validator;

	@BeforeEach
	void setUp() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void personalAccessTokenForm_requiresAllFields() {
		PersonalAccessTokenForm form = new PersonalAccessTokenForm();
		form.setName(" ");
		form.setDescription("desc");
		form.setExpiration(null);

		Set<ConstraintViolation<PersonalAccessTokenForm>> violations = validator.validate(form);

		assertTrue(hasViolation(violations, "name"));
		assertTrue(hasViolation(violations, "expiration"));
	}

	@Test
	void organizeCreateForm_rejectsInvalidNamePattern() {
		OrganizeCreateForm form = new OrganizeCreateForm();
		form.setName("org name");

		Set<ConstraintViolation<OrganizeCreateForm>> violations = validator.validate(form);

		assertTrue(hasViolation(violations, "name"));
	}

	@Test
	void repositoryCreateForm_requiresRepoName() {
		RepositoryCreateForm form = new RepositoryCreateForm();
		form.setRepoName(" ");

		Set<ConstraintViolation<RepositoryCreateForm>> violations = validator.validate(form);

		assertTrue(hasViolation(violations, "repoName"));
		assertFalse(hasViolation(violations, "ownerType"));
	}

	private boolean hasViolation(Set<? extends ConstraintViolation<?>> violations, String propertyName) {
		return violations.stream().anyMatch(v -> propertyName.equals(v.getPropertyPath().toString()));
	}
}
