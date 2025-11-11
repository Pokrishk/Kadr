package com.example.Kadr.config;

import com.example.Kadr.model.UserSettings;
import com.example.Kadr.service.UserSettingsService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class UserSettingsAdvice {

    private final UserSettingsService userSettingsService;

    @ModelAttribute
    public void contributeUserSettings(Model model, @AuthenticationPrincipal UserDetails principal) {
        Map<String, String> fontStacks = userSettingsService.getFontFamilyStacks();
        model.addAttribute("userFontStacks", fontStacks);

        if (principal == null) {
            model.addAttribute("userSettingsView", null);
            return;
        }

        UserSettings settings = userSettingsService.ensureSettingsForUsername(principal.getUsername());
        UserSettingsView view = UserSettingsView.from(settings, fontStacks, true);
        model.addAttribute("userSettingsView", view);
    }

    public record UserSettingsView(
            String theme,
            String fontFamily,
            int fontSize,
            int pageSize,
            ObjectNode savedFilters,
            Map<String, String> fontStacks,
            boolean authenticated
    ) {
        public static UserSettingsView from(UserSettings settings, Map<String, String> fontStacks, boolean authenticated) {
            ObjectNode filters = settings.getSavedFilters() == null
                    ? JsonNodeFactory.instance.objectNode()
                    : settings.getSavedFilters().deepCopy();
            return new UserSettingsView(
                    settings.getTheme(),
                    settings.getFontFamily(),
                    settings.getFontSize(),
                    settings.getPageSize(),
                    filters,
                    fontStacks,
                    authenticated
            );
        }
    }
}