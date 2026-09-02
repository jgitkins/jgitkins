package io.jgitkins.web.presentation.controller;

import io.jgitkins.web.application.contract.ExploreSummary;
import io.jgitkins.web.application.port.in.facade.ExploreFacadeUseCase;
import io.jgitkins.web.presentation.support.ExploreViewSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ExploreController {

	private final ExploreFacadeUseCase exploreFacadeUseCase;
	private final ExploreViewSupport exploreViewSupport;

	@GetMapping({ "/explore", "/explore/{type}" })
	public String explore(@PathVariable(value = "type", required = false) String type, Model model) {
		ExploreSummary summary = exploreFacadeUseCase.getExploreSummary(type);
		exploreViewSupport.populateModel(model, summary);
		return "explore/index";
	}
}
