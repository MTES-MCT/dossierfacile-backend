package fr.dossierfacile.api.front.das.mesri;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Single source of truth for "can this tenant use the MESR student status retrieval".
 * Used by the endpoint (403) and exposed to the frontend through {@code TenantModel.mesriStudentEligible},
 * so the button only shows when the call would be accepted.
 */
@Component
public class MesriStudentEligibility {

    public static final String FEATURE_FLAG_KEY = "das-mesri-student";

    @Value("${api-particulier.base-url:}")
    private String apiParticulierBaseUrl;

    private final FeatureFlagService featureFlagService;

    MesriStudentEligibility(String apiParticulierBaseUrl, FeatureFlagService featureFlagService) {
        this.apiParticulierBaseUrl = apiParticulierBaseUrl;
        this.featureFlagService = featureFlagService;
    }

    @Autowired
    MesriStudentEligibility(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    public boolean isEligible(Tenant tenant) {
        return ineligibilityReason(tenant).isEmpty();
    }

    /** @throws MesriNotEligibleException with the first failing rule */
    public void check(Tenant tenant) {
        ineligibilityReason(tenant).ifPresent(reason -> {
            throw new MesriNotEligibleException(reason);
        });
    }

    private Optional<String> ineligibilityReason(Tenant tenant) {
        if (StringUtils.isBlank(apiParticulierBaseUrl)) {
            return Optional.of("API Particulier is not configured on this environment");
        }
        if (tenant == null || tenant.getId() == null) {
            return Optional.of("Unknown tenant");
        }
        if (!Boolean.TRUE.equals(tenant.getFranceConnect())) {
            return Optional.of("Tenant is not FranceConnected");
        }
        if (tenant.getApartmentSharing() == null || tenant.getApartmentSharing().getApplicationType() != ApplicationType.ALONE) {
            return Optional.of("Only ALONE applications are supported by this POC");
        }
        // Checked last: this call may assign the user to the rollout, no need to do it for ineligible tenants
        if (!featureFlagService.isFeatureEnabledForUser(tenant.getId(), FEATURE_FLAG_KEY)) {
            return Optional.of("Feature " + FEATURE_FLAG_KEY + " is not enabled for this tenant");
        }
        return Optional.empty();
    }
}
