package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.Log;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fr.dossierfacile.common.enums.LogType.ACCOUNT_DENIED;
import static fr.dossierfacile.common.enums.LogType.ACCOUNT_VALIDATED;
import static lombok.AccessLevel.PRIVATE;

@Getter
@AllArgsConstructor(access = PRIVATE)
public class TenantInfoHeader {

    private final List<HeaderElement> elements;

    public static TenantInfoHeader build(Tenant tenant, List<UserApi> partners, List<Log> tenantLogs) {
        List<HeaderElement> elements = List.of(
                new HeaderElement("Nom", tenant.getFullName()),
                new HeaderElement("Dossier", getApplicationType(tenant)),
                new HeaderElement("Partenaires", getPartnerNames(partners)),
                new HeaderElement("Passages en BO", getTimesTenantAppearedInBoSinceLastValidation(tenantLogs))
        );
        return new TenantInfoHeader(elements);
    }

    private static String getPartnerNames(List<UserApi> partners) {
        return partners.stream()
                .map(partner -> partner.getName2() != null ? partner.getName2() : partner.getName())
                .collect(Collectors.joining(", "));
    }

    private static String getApplicationType(Tenant tenant) {
        return switch (tenant.getApartmentSharing().getApplicationType()) {
            case ALONE -> "Seul·e";
            case COUPLE -> "En couple";
            case GROUP -> "En colocation";
        };
    }

    private static String getTimesTenantAppearedInBoSinceLastValidation(List<Log> tenantLogs) {
        Stream<Log> deniedSinceLastValidation = tenantLogs.stream()
                .filter(log -> List.of(ACCOUNT_DENIED, ACCOUNT_VALIDATED).contains(log.getLogType()))
                .sorted(Comparator.comparing(Log::getCreationDateTime).reversed())
                .takeWhile(log -> log.getLogType() == ACCOUNT_DENIED);
        return String.valueOf(deniedSinceLastValidation.count() + 1);
    }

    @Getter
    @ToString
    @EqualsAndHashCode
    @AllArgsConstructor
    static class HeaderElement {
        private final String label;
        private final String value;
    }

}
