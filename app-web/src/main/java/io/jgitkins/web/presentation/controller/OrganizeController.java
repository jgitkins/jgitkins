package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.contract.OrganizeCreateResult;
import io.jgitkins.web.application.port.in.facade.OrganizeCreateFacadeUseCase;
import io.jgitkins.web.presentation.contract.OrganizeCreateForm;
import io.jgitkins.web.presentation.support.OrganizeCreateViewSupport;
import io.jgitkins.web.presentation.support.SessionUserSupport;
import jakarta.validation.Valid;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class OrganizeController {

	private final OrganizeCreateFacadeUseCase organizeCreateFacadeUseCase;
	private final OrganizeCreateViewSupport organizeCreateViewSupport;
	private final SessionUserSupport sessionUserSupport;

	@GetMapping("/organizes/new")
	public String newOrganize(Model model) {
		OrganizeCreateForm form = new OrganizeCreateForm();
		model.addAttribute("form", form);
		return "organizes/new";
	}

	@PostMapping("/organizes")
	public String createOrganize(@Valid @ModelAttribute("form") OrganizeCreateForm form,
			BindingResult bindingResult,
			Model model) {
		if (bindingResult.hasErrors()) {
			model.addAttribute("formError", bindingResult.getFieldError() != null
					? bindingResult.getFieldError().getDefaultMessage()
					: "요청이 올바르지 않습니다.");
			return "organizes/new";
		}

		Optional<Long> ownerId = sessionUserSupport.resolveUserId();
		if (ownerId.isEmpty()) {
			model.addAttribute("formError", organizeCreateViewSupport.getMessage("error.auth.login_required"));
			return "organizes/new";
		}

		OrganizeCreateResult result = organizeCreateFacadeUseCase.createOrganize(
				organizeCreateViewSupport.toRequest(form, ownerId.get()));

		if (result.errorMessage() != null) {
			model.addAttribute("formError", result.errorMessage());
			return "organizes/new";
		}

		return "redirect:/";
	}
}
