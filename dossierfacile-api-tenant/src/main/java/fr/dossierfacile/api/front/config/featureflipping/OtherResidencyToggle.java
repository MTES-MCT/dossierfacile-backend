package fr.dossierfacile.api.front.config.featureflipping;

import fr.dossierfacile.common.utils.DateBasedFeatureToggle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
// TODO delete after activation
public class OtherResidencyToggle extends DateBasedFeatureToggle {

    public OtherResidencyToggle(@Value("${categories.otherresidency.activation.date:}") String otherResidencyActivationDate) {
        super(otherResidencyActivationDate);
    }

}
