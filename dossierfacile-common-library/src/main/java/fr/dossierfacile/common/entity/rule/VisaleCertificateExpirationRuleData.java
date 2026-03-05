package fr.dossierfacile.common.entity.rule;

import java.time.LocalDate;

public record VisaleCertificateExpirationRuleData(LocalDate extractedExpirationDate) implements RuleData {

    public static final String R_VISALE_CERTIFICATE_EXPIRATION = "R_VISALE_CERTIFICATE_EXPIRATION";

    @Override
    public String getType() {
        return R_VISALE_CERTIFICATE_EXPIRATION;
    }
}
