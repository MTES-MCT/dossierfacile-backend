package fr.dossierfacile.common.enums;

import lombok.Getter;

@Getter
public enum DocumentCategory {
    IDENTIFICATION("tenant.profile.link1.v2"),
    RESIDENCY("tenant.profile.link2.v2"),
    PROFESSIONAL("tenant.profile.link3.v2"),
    FINANCIAL("tenant.profile.link5.v2"),
    TAX("tenant.profile.link4.v2"),
    IDENTIFICATION_LEGAL_PERSON("tenant.profile.link1.v2");

    String label;

    DocumentCategory(String label) {
        this.label = label;
    }
}
