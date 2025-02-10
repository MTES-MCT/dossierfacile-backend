package fr.dossierfacile.api.dossierfacileapiowner.annotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ValidDpeDateValidator implements ConstraintValidator<ValidDpeDate, String> {

    @Override
    public boolean isValid(String dpeDate, ConstraintValidatorContext context) {
        if (dpeDate == null) {
            return true;
        }
        try {
            var parsedDate = LocalDate.parse(dpeDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            var currentDate = LocalDate.now();
            return !parsedDate.isAfter(currentDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
