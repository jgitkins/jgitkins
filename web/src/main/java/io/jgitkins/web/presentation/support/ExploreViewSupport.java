package io.jgitkins.web.presentation.support;

import io.jgitkins.web.application.dto.ExploreSummary;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class ExploreViewSupport {

    public void populateModel(Model model, ExploreSummary summary) {
        model.addAttribute("exploreType", summary.exploreType());

        if (summary.repositories() != null) {
            model.addAttribute("repositories", summary.repositories());
        }
        if (summary.organizations() != null) {
            model.addAttribute("organizations", summary.organizations());
        }
        if (summary.users() != null) {
            model.addAttribute("users", summary.users());
        }
    }
}
