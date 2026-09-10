package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.entity.UserApi;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.gouv.bo.service.TenantService;
import fr.gouv.bo.service.UserApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOFeatureFlagsControllerTest {

    private static final String KEY = OperatorReviewPolicy.PARTNER_COMPLETED_OPTIN_FEATURE_FLAG;

    @Mock
    private FeatureFlagService featureFlagService;
    @Mock
    private TenantService tenantService;
    @Mock
    private LotteryDrawService lotteryDrawService;
    @Mock
    private UserApiService userApiService;

    private BOFeatureFlagsController controller;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
    void setUp() {
        controller = new BOFeatureFlagsController(featureFlagService, tenantService, lotteryDrawService, userApiService);
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Test
    void updateOptedInPartners_savesKnownPartners() {
        FeatureFlag flag = FeatureFlag.builder().key(KEY).build();
        when(featureFlagService.getFeatureFlag(KEY)).thenReturn(flag);
        when(userApiService.findByName(anyString())).thenReturn(Optional.of(new UserApi()));

        String view = controller.updateOptedInPartners(KEY, " dfconnect-ics, other ", redirectAttributes);

        assertThat(view).isEqualTo("redirect:/bo/feature-flags");
        verify(featureFlagService).updateOptedInPartners(eq(flag), eq(Set.of("dfconnect-ics", "other")));
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("successMessage");
    }

    @Test
    void updateOptedInPartners_refusesUnknownPartnerAndSavesNothing() {
        when(userApiService.findByName("dfconnect-ics")).thenReturn(Optional.of(new UserApi()));
        when(userApiService.findByName("typo")).thenReturn(Optional.empty());

        controller.updateOptedInPartners(KEY, "dfconnect-ics,typo", redirectAttributes);

        verify(featureFlagService, never()).updateOptedInPartners(any(), any());
        assertThat(redirectAttributes.getFlashAttributes().get("errorMessage").toString()).contains("typo");
    }

    @Test
    void updateOptedInPartners_refusesTheOwnerPartner() {
        controller.updateOptedInPartners(KEY, "dfconnect-proprietaire", redirectAttributes);

        verify(featureFlagService, never()).updateOptedInPartners(any(), any());
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }

    @Test
    void updateOptedInPartners_refusesNonPartnerScopedFlags() {
        controller.updateOptedInPartners("tenant_completed_optin", "dfconnect-ics", redirectAttributes);

        verify(featureFlagService, never()).updateOptedInPartners(any(), any());
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }
}
