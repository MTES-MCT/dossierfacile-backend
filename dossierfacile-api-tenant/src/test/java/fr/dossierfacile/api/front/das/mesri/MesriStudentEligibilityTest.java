package fr.dossierfacile.api.front.das.mesri;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class MesriStudentEligibilityTest {

    private FeatureFlagService featureFlagService;
    private MesriStudentEligibility eligibility;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        featureFlagService = mock(FeatureFlagService.class);
        eligibility = new MesriStudentEligibility("https://staging.particulier.api.gouv.fr", featureFlagService);
        tenant = new Tenant();
        tenant.setId(42L);
        tenant.setFranceConnect(true);
        tenant.setApartmentSharing(ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build());
    }

    @Test
    void eligibleWhenAllRulesPass() {
        when(featureFlagService.isFeatureEnabledForUser(42L, MesriStudentEligibility.FEATURE_FLAG_KEY)).thenReturn(true);

        assertThat(eligibility.isEligible(tenant)).isTrue();
        eligibility.check(tenant);
    }

    @Test
    void notEligibleWhenFlagIsOff() {
        when(featureFlagService.isFeatureEnabledForUser(42L, MesriStudentEligibility.FEATURE_FLAG_KEY)).thenReturn(false);

        assertThat(eligibility.isEligible(tenant)).isFalse();
        assertThatThrownBy(() -> eligibility.check(tenant)).isInstanceOf(MesriNotEligibleException.class)
                .hasMessageContaining("das-mesri-student");
    }

    @Test
    void notEligibleWhenNotFranceConnectedAndFlagIsNotEvenChecked() {
        tenant.setFranceConnect(false);

        assertThat(eligibility.isEligible(tenant)).isFalse();
        verify(featureFlagService, never()).isFeatureEnabledForUser(anyLong(), any(String.class));
    }

    @Test
    void notEligibleForCoupleApplications() {
        tenant.getApartmentSharing().setApplicationType(ApplicationType.COUPLE);

        assertThatThrownBy(() -> eligibility.check(tenant)).isInstanceOf(MesriNotEligibleException.class)
                .hasMessageContaining("ALONE");
    }

    @Test
    void notEligibleWhenApiParticulierIsNotConfigured() {
        MesriStudentEligibility unconfigured = new MesriStudentEligibility("", featureFlagService);

        assertThat(unconfigured.isEligible(tenant)).isFalse();
        verify(featureFlagService, never()).isFeatureEnabledForUser(anyLong(), any(String.class));
    }
}
