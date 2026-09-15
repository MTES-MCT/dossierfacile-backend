package fr.gouv.bo.controller;

import fr.dossierfacile.common.entity.FeatureFlag;
import fr.dossierfacile.common.service.interfaces.FeatureFlagService;
import fr.dossierfacile.common.service.interfaces.LotteryDrawService;
import fr.dossierfacile.common.service.interfaces.OperatorReviewPolicy;
import fr.gouv.bo.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BOFeatureFlagsControllerTest {

    @Mock
    private FeatureFlagService featureFlagService;
    @Mock
    private TenantService tenantService;
    @Mock
    private LotteryDrawService lotteryDrawService;

    private BOFeatureFlagsController controller;
    private RedirectAttributesModelMap redirectAttributes;

    @BeforeEach
    void setUp() {
        controller = new BOFeatureFlagsController(featureFlagService, tenantService, lotteryDrawService);
        redirectAttributes = new RedirectAttributesModelMap();
    }

    @Test
    void updateRollout_refusesGlobalFlags() {
        String view = controller.updateRollout(OperatorReviewPolicy.PARTNER_COMPLETED_OPTIN_FEATURE_FLAG, 50, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/bo/feature-flags");
        verify(featureFlagService, never()).updateRolloutForFeatureFlag(any(), anyInt());
        assertThat(redirectAttributes.getFlashAttributes()).containsKey("errorMessage");
    }

}
