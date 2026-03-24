package io.jgitkins.web.presentation.support;

import io.jgitkins.web.application.dto.NamespaceSummary;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
public class NamespaceViewSupport {

    public void populateModel(Model model, NamespaceSummary summary, String activeTab) {
        model.addAttribute("detail", summary);
        if (activeTab != null) {
            model.addAttribute("activeTab", activeTab);
        }
    }
}
