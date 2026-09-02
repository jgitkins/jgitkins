package io.jgitkins.web.presentation.support;

import io.jgitkins.web.application.contract.OrganizeCreateRequest;
import io.jgitkins.web.presentation.contract.OrganizeCreateForm;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrganizeCreateViewSupport {

    private final MessageSource messageSource;

    public OrganizeCreateRequest toRequest(OrganizeCreateForm form, Long ownerId) {
        return new OrganizeCreateRequest(
                form.getName(),
                ownerId,
                form.getDescription());
    }

    public String getMessage(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }
}
