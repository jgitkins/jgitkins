package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.dto.NamespaceSummary;
import io.jgitkins.web.application.port.in.facade.NamespaceFacadeUseCase;
import io.jgitkins.web.presentation.support.NamespaceViewSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class NamespaceDetailController {

	private final NamespaceFacadeUseCase namespaceFacadeUseCase;
	private final NamespaceViewSupport namespaceViewSupport;

	@GetMapping("/{namespace}")
	public String namespaceDetail(@PathVariable("namespace") String namespace, Model model) {
		NamespaceSummary summary = namespaceFacadeUseCase.getNamespaceSummary(namespace);
		if (summary.organization()) {
			namespaceViewSupport.populateModel(model, summary, null);
			return "namespaces/organization-detail";
		}
		return "redirect:/" + namespace + "/-/repositories";
	}

	@GetMapping("/{namespace}/-/repositories")
	public String userRepositories(@PathVariable("namespace") String namespace, Model model) {
		NamespaceSummary summary = namespaceFacadeUseCase.getNamespaceSummary(namespace);
		namespaceViewSupport.populateModel(model, summary, "repositories");
		return "namespaces/user-detail";
	}

	@GetMapping("/{namespace}/-/stars")
	public String userStars(@PathVariable("namespace") String namespace, Model model) {
		NamespaceSummary summary = namespaceFacadeUseCase.getNamespaceSummary(namespace);
		namespaceViewSupport.populateModel(model, summary, "stars");
		return "namespaces/user-stars";
	}
}
