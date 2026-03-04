package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BOFeatureFlagsController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/bo/feature-flags")
    public String featureFlags(Model model) {
        model.addAttribute("featureFlags", featureFlagService.getAllFeatureFlags());
        return "bo/feature-flags";
    }

    @PostMapping("/bo/feature-flags/toggle")
    public String toggleBox(@RequestParam("key") String key) {
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        featureFlagService.toggleFeatureFlag(featureFlag, !featureFlag.isActive());
        return "redirect:/bo/feature-flags";
    }

    @PostMapping("/bo/feature-flags/rollout")
    public String updateRollout(@RequestParam("key") String key, @RequestParam("value") int value) {
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        int newValue = value;
        if (newValue < 0) newValue = 0;
        if (newValue > 100) newValue = 100;
        featureFlagService.updateRolloutForFeatureFlag(featureFlag, newValue);
        return "redirect:/bo/feature-flags";
    }

}