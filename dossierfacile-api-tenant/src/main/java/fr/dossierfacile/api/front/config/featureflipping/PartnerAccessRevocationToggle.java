package fr.dossierfacile.api.front.config.featureflipping;

import fr.dossierfacile.common.utils.DateBasedFeatureToggle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
// TODO delete after activation
public class PartnerAccessRevocationToggle extends DateBasedFeatureToggle {

    public PartnerAccessRevocationToggle(@Value("${toggle.partners.revocation.activation.date:}") String activationDate) {
        super(activationDate);
    }

}
