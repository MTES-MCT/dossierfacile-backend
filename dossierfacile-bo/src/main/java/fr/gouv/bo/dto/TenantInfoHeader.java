package fr.gouv.bo.dto;

import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.UserApi;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public class TenantInfoHeader {

    private final List<HeaderElement> elements;

    public static TenantInfoHeader build(Tenant tenant, List<UserApi> partners) {
        List<HeaderElement> elements = List.of(
                new HeaderElement("Nom", tenant.getFullName()),
                new HeaderElement("Dossier", getApplicationType(tenant)),
                new HeaderElement("Partenaires", getPartnerNames(partners))
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

    @Getter
    @AllArgsConstructor
    private static class HeaderElement {
        private final String label;
        private final String value;
    }

}
