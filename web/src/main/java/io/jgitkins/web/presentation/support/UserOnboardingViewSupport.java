package io.jgitkins.web.presentation.support;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserOnboardingViewSupport {

    private final MessageSource messageSource;
    private final SessionSupport sessionSupport;

    public void storeError(jakarta.servlet.http.HttpServletRequest request, String message) {
        sessionSupport.storeUsernameSetupError(request, message);
    }

    public String getMessage(String code) {
        return messageSource.getMessage(code, null, code, LocaleContextHolder.getLocale());
    }
}
