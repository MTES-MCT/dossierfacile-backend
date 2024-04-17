package fr.dossierfacile.common.enums;

import lombok.Getter;

@Getter
public enum DocumentCategory {

    IDENTIFICATION("tenant.profile.link1.v2", "Identité"),
    RESIDENCY("tenant.profile.link2.v2", "Hébergement"),
    PROFESSIONAL("tenant.profile.link3.v2", "Situation_professionnelle"),
    FINANCIAL("tenant.profile.link5.v2", "Justificatif_ressources"),
    TAX("tenant.profile.link4.v2", "Imposition"),

    IDENTIFICATION_LEGAL_PERSON("tenant.profile.link1.v2", "Identité"),
    GUARANTEE_PROVIDER_CERTIFICATE("tenant.profile.link9.v2", "Certificat_de_garantie"),

    NULL("", "");

    final String label;
    final String text;

    DocumentCategory(String label, String text) {
        this.label = label;
        this.text = text;
    }
}
