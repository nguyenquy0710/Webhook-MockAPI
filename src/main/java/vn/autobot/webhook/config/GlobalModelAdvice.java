package vn.autobot.webhook.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.autobot.webhook.service.SettingsService;

/** Settings every page needs (header brand, register link). */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final SettingsService settings;

    @ModelAttribute("siteName")
    public String siteName() {
        return settings.getSiteName();
    }

    @ModelAttribute("registrationEnabled")
    public boolean registrationEnabled() {
        return settings.isRegistrationEnabled();
    }
}
