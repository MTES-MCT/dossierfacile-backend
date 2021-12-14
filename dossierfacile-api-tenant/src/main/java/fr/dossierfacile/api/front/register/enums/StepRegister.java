package fr.dossierfacile.api.front.register.enums;

import lombok.Getter;

@Getter
public enum StepRegister {
    ACCOUNT_PARTNER_API("accountApiPartner"),
    ACCOUNT("account"),
    NAMES("names"),
    APPLICATION("application"),
    HONOR_DECLARATION("honorDeclaration"),
    DOCUMENT_IDENTIFICATION("documentIdentification"),
    DOCUMENT_RESIDENCY("documentResidency"),
    DOCUMENT_PROFESSIONAL("documentProfessional"),
    DOCUMENT_FINANCIAL("documentFinancial"),
    DOCUMENT_TAX("documentTax"),
    NAME_GUARANTOR_NATURAL_PERSON("nameGuarantorNaturalPerson"),
    DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON_FILE("documentIdentificationGuarantorNaturalPersonFile"),
    DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON("documentIdentificationGuarantorNaturalPerson"),
    DOCUMENT_RESIDENCY_GUARANTOR_NATURAL_PERSON("documentResidencyGuarantorNaturalPerson"),
    DOCUMENT_PROFESSIONAL_GUARANTOR_NATURAL_PERSON("documentProfessionalGuarantorNaturalPerson"),
    DOCUMENT_FINANCIAL_GUARANTOR_NATURAL_PERSON("documentFinancialGuarantorNaturalPerson"),
    DOCUMENT_TAX_GUARANTOR_NATURAL_PERSON("documentTaxGuarantorNaturalPerson"),
    DOCUMENT_IDENTIFICATION_GUARANTOR_ORGANISM("documentIdentificationGuarantorOrganism"),
    DOCUMENT_IDENTIFICATION_GUARANTOR_LEGAL_PERSON("documentIdentificationGuarantorLegalPerson"),
    DOCUMENT_IDENTIFICATION_REPRESENTAN_GUARANTOR_LEGAL_PERSON("documentIdentificationRepresentanGuarantorLegalPerson"),
    GUARANTOR_TYPE("guarantorType");
    private final String label;

    StepRegister(String label) {
        this.label = label;
    }
}
