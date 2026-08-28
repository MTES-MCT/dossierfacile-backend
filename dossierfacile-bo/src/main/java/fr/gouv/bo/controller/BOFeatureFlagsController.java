package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.service.interfaces.CompletedEligibilityService;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.gouv.bo.service.TenantService;
import lombok.RequiredArgsConstructor;

import java.util.List;
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

    private static final String REDIRECT_FEATURE_FLAGS = "redirect:/bo/feature-flags";

    private final FeatureFlagService featureFlagService;
    private final TenantService tenantService;
    private final LotteryDrawService lotteryDrawService;

    @GetMapping("/bo/feature-flags")
    public String featureFlags(Model model) {
        List<FeatureFlag> featureFlags = featureFlagService.getAllFeatureFlags();
        model.addAttribute("featureFlags", featureFlags);
        // The COMPLETED rollback is a full rollback only: allowed once the flag is deactivated or at 0%
        boolean completedRollbackAllowed = featureFlags.stream()
                .filter(flag -> CompletedEligibilityService.COMPLETED_OPTIN_FEATURE_FLAG.equals(flag.getKey()))
                .findFirst()
                .map(flag -> !flag.isActive() || flag.getRolloutPct() == 0)
                .orElse(false);
        model.addAttribute("completedRollbackAllowed", completedRollbackAllowed);
        return "bo/feature-flags";
    }

    @PostMapping("/bo/feature-flags/toggle")
    public String toggleBox(@RequestParam("key") String key) {
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        boolean activate = !featureFlag.isActive();
        featureFlagService.toggleFeatureFlag(featureFlag, activate);
        // Lottery kill-switch: deactivation flushes pending applications to the queue
        if (LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG.equals(key) && !activate) {
            int flushed = lotteryDrawService.flushPendingTicketsToProcessing();
            log.info("Lottery deactivated: {} pending applications flushed to TO_PROCESS", flushed);
        }
        return REDIRECT_FEATURE_FLAGS;
    }

    @PostMapping("/bo/feature-flags/rollout")
    public String updateRollout(@RequestParam("key") String key, @RequestParam("value") int value) {
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        int newValue = value;
        if (newValue < 0) newValue = 0;
        if (newValue > 100) newValue = 100;
        featureFlagService.updateRolloutForFeatureFlag(featureFlag, newValue);
        return REDIRECT_FEATURE_FLAGS;
    }

    // Rollback action for the COMPLETED opt-in MVP: sends every COMPLETED dossier back
    // to the operator queue. Meant to be run manually after lowering/deactivating the flag.
    @PostMapping("/bo/feature-flags/completed-rollback")
    public String rollbackCompletedDossiers() {
        int count = tenantService.switchCompletedDossiersBackToProcessing();
        log.info("COMPLETED opt-in rollback: {} dossiers switched back to TO_PROCESS", count);
        return REDIRECT_FEATURE_FLAGS;
    }

}