package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.TenantLog;
import fr.gouv.bo.utils.DateFormatUtil;
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
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

@Getter
@AllArgsConstructor(access = PRIVATE)
public class TenantInfoHeader {

    private final List<HeaderElement> elements;

    public static TenantInfoHeader build(Tenant tenant, List<UserApi> partners, List<TenantLog> tenantLogs) {
        List<HeaderElement> elements = List.of(
                new HeaderElement("FranceConnecté", (tenant.getFranceConnect() == Boolean.TRUE) ? "Oui" : "Non"),
                new HeaderElement("Nom", tenant.getFullName()),
                new HeaderElement("Dossier", getApplicationType(tenant)),
                new HeaderElement("Partenaires", getPartnerNames(partners)),
                new HeaderElement("Historique", getCurrentStateBasedOnLogs(tenantLogs))
        );
        return new TenantInfoHeader(elements);
    }

    private static String getPartnerNames(List<UserApi> partners) {
        return partners.stream()
                .map(partner -> partner.getName2() != null ? partner.getName2() : partner.getName())
                .collect(Collectors.joining(", "));
    }

    private static String getApplicationType(Tenant tenant) {
        ApartmentSharing apartmentSharing = tenant.getApartmentSharing();
        String cotenantNames = apartmentSharing.getTenants().stream()
                .filter(cotenant -> !cotenant.getId().equals(tenant.getId()))
                .map(User::getFullName)
                .collect(Collectors.joining(", "));
        return switch (apartmentSharing.getApplicationType()) {
            case ALONE -> "Seul·e";
            case COUPLE -> "En couple avec " + cotenantNames;
            case GROUP -> "En colocation avec " + cotenantNames;
        };
    }

    private static String getCurrentStateBasedOnLogs(List<TenantLog> tenantLogs) {
        List<TenantLog> validatedAndDeniedLogs = tenantLogs.stream()
                .filter(log -> List.of(ACCOUNT_DENIED, ACCOUNT_VALIDATED).contains(log.getLogType()))
                .sorted(Comparator.comparing(TenantLog::getCreationDateTime).reversed())
                .toList();
        if (validatedAndDeniedLogs.isEmpty()) {
            return "Premier passage";
        }
        TenantLog lastLog = validatedAndDeniedLogs.get(0);
        if (lastLog.getLogType() == ACCOUNT_VALIDATED) {
            return format("Validé %s", DateFormatUtil.formatRelativeToNow(lastLog.getCreationDateTime()));
        }
        Stream<TenantLog> deniedSinceLastValidation = validatedAndDeniedLogs
                .stream()
                .takeWhile(log -> log.getLogType() == ACCOUNT_DENIED);
        return format("Refusé %s fois", deniedSinceLastValidation.count());
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
