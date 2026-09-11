package fr.gouv.bo.controller;

import fr.dossierfacile.common.constants.PartnerConstants;
import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.common.service.interfaces.LotteryTicketService;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BOFeatureFlagsController {

    private static final String REDIRECT_FEATURE_FLAGS = "redirect:/bo/feature-flags";

    /**
     * Global on/off flags, read through FeatureFlagService.isFeatureEnabled: rollout_pct and
     * only_for_new_user are ignored, so the rollout edition is hidden and refused for them.
     */
    private static final Set<String> GLOBAL_FLAG_KEYS = Set.of(
            LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG,
            OperatorReviewPolicy.PARTNER_COMPLETED_OPTIN_FEATURE_FLAG);

    /**
     * Partner-scoped flags: the opted-in partner list (user_api.name) is editable.
     * TODO(partner-completed-optin-100): remove with the partner_completed_optin flag (this set, the
     * "partners" model attribute, updateOptedInPartners and the modal in feature-flags.html).
     */
    private static final Set<String> PARTNER_SCOPED_FLAG_KEYS = Set.of(OperatorReviewPolicy.PARTNER_COMPLETED_OPTIN_FEATURE_FLAG);

    private final FeatureFlagService featureFlagService;
    private final TenantService tenantService;
    private final LotteryDrawService lotteryDrawService;
    private final UserApiService userApiService;

    @GetMapping("/bo/feature-flags")
    public String featureFlags(Model model) {
        List<FeatureFlag> featureFlags = featureFlagService.getAllFeatureFlags();
        model.addAttribute("featureFlags", featureFlags);
        // The COMPLETED rollback is a full rollback only: allowed once the flag is deactivated or at 0%
        // TODO remove this block once optin is rolled out to 100% 
        boolean completedRollbackAllowed = featureFlags.stream()
                .filter(flag -> OperatorReviewPolicy.COMPLETED_OPTIN_FEATURE_FLAG.equals(flag.getKey()))
                .findFirst()
                .map(flag -> !flag.isActive() || flag.getRolloutPct() == 0)
                .orElse(false);
        model.addAttribute("completedRollbackAllowed", completedRollbackAllowed);
        model.addAttribute("globalFlagKeys", GLOBAL_FLAG_KEYS);
        model.addAttribute("partnerScopedFlagKeys", PARTNER_SCOPED_FLAG_KEYS);
        model.addAttribute("partners", userApiService.findAll());
        return "bo/feature-flags";
    }

    @PostMapping("/bo/feature-flags/opted-in-partners")
    public String updateOptedInPartners(@RequestParam("key") String key, @RequestParam("value") String value,
                                        RedirectAttributes redirectAttributes) {
        if (!PARTNER_SCOPED_FLAG_KEYS.contains(key)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Le flag " + key + " n'est pas un flag partenaire : il n'a pas de liste de partenaires.");
            return REDIRECT_FEATURE_FLAGS;
        }
        Set<String> names = FeatureFlag.builder().optedInPartners(value).build().getOptedInPartnerNames();
        if (names.contains(PartnerConstants.DF_OWNER_NAME)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Le partenaire " + PartnerConstants.DF_OWNER_NAME + " (espace propriétaire) ne peut pas intégrer le statut COMPLETED.");
            return REDIRECT_FEATURE_FLAGS;
        }
        // Every name must be an existing partner (user_api.name): nothing is saved otherwise
        List<String> unknownNames = names.stream()
                .filter(name -> userApiService.findByName(name).isEmpty())
                .toList();
        if (!unknownNames.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Partenaire(s) inconnu(s), liste non enregistrée : " + String.join(", ", unknownNames));
            return REDIRECT_FEATURE_FLAGS;
        }
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        featureFlagService.updateOptedInPartners(featureFlag, names);
        redirectAttributes.addFlashAttribute("successMessage",
                "Partenaires ayant intégré le statut COMPLETED (flag " + key + ") : " + (names.isEmpty() ? "aucun" : String.join(", ", names)));
        return REDIRECT_FEATURE_FLAGS;
    }

    @PostMapping("/bo/feature-flags/toggle")
    public String toggleBox(@RequestParam("key") String key) {
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        boolean activate = !featureFlag.isActive();
        featureFlagService.toggleFeatureFlag(featureFlag, activate);
        if (LotteryTicketService.TENANT_LOTTERY_FEATURE_FLAG.equals(key)) {
            if (activate) {
                // Opt-ins already in the queue keep their place: they bypass the lottery
                int granted = lotteryDrawService.grantTicketsToQueuedOptIns();
                log.info("Lottery activated: {} queued opt-ins granted a DRAWN ticket", granted);
            } else {
                // Kill-switch: deactivation flushes pending applications to the queue
                int flushed = lotteryDrawService.flushPendingTicketsToProcessing();
                log.info("Lottery deactivated: {} pending applications flushed to TO_PROCESS", flushed);
            }
        }
        return REDIRECT_FEATURE_FLAGS;
    }

    @PostMapping("/bo/feature-flags/rollout")
    public String updateRollout(@RequestParam("key") String key, @RequestParam("value") int value,
                                RedirectAttributes redirectAttributes) {
        if (GLOBAL_FLAG_KEYS.contains(key)) {
            // The button is hidden in the UI: refuse a direct POST too, it would only trigger
            // a useless recomputation of the user assignments
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Le flag " + key + " est global (on/off) : son pourcentage de rollout n'est pas modifiable.");
            return REDIRECT_FEATURE_FLAGS;
        }
        FeatureFlag featureFlag = featureFlagService.getFeatureFlag(key);
        int newValue = value;
        if (newValue < 0) newValue = 0;
        if (newValue > 100) newValue = 100;
        featureFlagService.updateRolloutForFeatureFlag(featureFlag, newValue);
        return REDIRECT_FEATURE_FLAGS;
    }

    // Rollback action for the COMPLETED opt-in MVP: sends every COMPLETED dossier back
    // to the operator queue. Meant to be run manually after lowering/deactivating the flag.
    // TODO(completed-optin-rollout-100): remove together with the flag check (see above)
    @PostMapping("/bo/feature-flags/completed-rollback")
    public String rollbackCompletedDossiers() {
        int count = tenantService.switchCompletedDossiersBackToProcessing();
        log.info("COMPLETED opt-in rollback: {} dossiers switched back to TO_PROCESS", count);
        return REDIRECT_FEATURE_FLAGS;
    }

}