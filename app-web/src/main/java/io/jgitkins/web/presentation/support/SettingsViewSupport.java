package io.jgitkins.web.presentation.support;

import io.jgitkins.web.application.dto.UserCredentialIssueRequest;
import io.jgitkins.web.presentation.dto.PersonalAccessTokenForm;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettingsViewSupport {

    private final MessageSource messageSource;

    public UserCredentialIssueRequest toIssueRequest(PersonalAccessTokenForm form) {
        return new UserCredentialIssueRequest(
                form.getName(),
                form.getDescription(),
                form.getExpiration());
    }

    public String getMessage(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }
}
