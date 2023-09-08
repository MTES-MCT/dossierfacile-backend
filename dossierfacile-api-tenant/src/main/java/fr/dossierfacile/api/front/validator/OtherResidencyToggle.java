package fr.dossierfacile.api.front.validator;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
// TODO delete after activation
public class OtherResidencyToggle {

    private final String otherResidencyActivationDate;

    public OtherResidencyToggle(@Value("${categories.otherresidency.activation.date:}") String otherResidencyActivationDate) {
        this.otherResidencyActivationDate = otherResidencyActivationDate;
    }

    public boolean isNotActive() {
        if (StringUtils.isBlank(otherResidencyActivationDate)) {
            return false;
        }
        LocalDate activationDate = LocalDate.parse(otherResidencyActivationDate);
        return LocalDate.now().isBefore(activationDate);
    }

    public String getActivationDate() {
        return otherResidencyActivationDate;
    }

}
