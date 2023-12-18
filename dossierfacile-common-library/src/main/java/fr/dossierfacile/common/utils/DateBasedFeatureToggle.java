package fr.dossierfacile.common.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateBasedFeatureToggle {

    private final LocalDate activationDate;

    public DateBasedFeatureToggle(String activationDate) {
        this.activationDate = activationDate.isBlank() ? null : LocalDate.parse(activationDate);
    }

    public boolean isActive() {
        if (activationDate == null) {
            return true;
        }
        return LocalDate.now().plusDays(1).isAfter(activationDate);
    }

    public boolean isNotActive() {
        return !isActive();
    }

    public String getActivationDate() {
        return activationDate.format(DateTimeFormatter.ISO_DATE);
    }

}
