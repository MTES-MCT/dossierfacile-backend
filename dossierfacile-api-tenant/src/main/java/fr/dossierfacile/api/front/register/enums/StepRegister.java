package fr.dossierfacile.api.front.register.enums;

import fr.dossierfacile.api.front.register.guarantor.legal_person.DocumentIdentificationGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.legal_person.DocumentIdentificationRepresentanGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.legal_person.NameGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentFinancialGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonFile;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentProfessionalGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentResidencyGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentTaxGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.NameGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.organism.DocumentIdentificationGuarantorOrganism;
import fr.dossierfacile.api.front.register.tenant.*;
import lombok.Getter;

@Getter
public enum StepRegister {
    ACCOUNT_PARTNER_API(AccountApiPartner.class.getName()),
    ACCOUNT(Account.class.getName()),
    NAMES(Names.class.getName()),
    APPLICATION_V1(ApplicationV1.class.getName()),
    APPLICATION(Application.class.getName()),
    HONOR_DECLARATION(HonorDeclaration.class.getName()),
    DOCUMENT_IDENTIFICATION(DocumentIdentification.class.getName()),
    DOCUMENT_RESIDENCY(DocumentResidency.class.getName()),
    DOCUMENT_PROFESSIONAL(DocumentProfessional.class.getName()),
    DOCUMENT_FINANCIAL(DocumentFinancial.class.getName()),
    DOCUMENT_TAX(DocumentTax.class.getName()),
    NAME_GUARANTOR_NATURAL_PERSON(NameGuarantorNaturalPerson.class.getName()),
    DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON_FILE(DocumentIdentificationGuarantorNaturalPersonFile.class.getName()),
    DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON(DocumentIdentificationGuarantorNaturalPerson.class.getName()),
    DOCUMENT_RESIDENCY_GUARANTOR_NATURAL_PERSON(DocumentResidencyGuarantorNaturalPerson.class.getName()),
    DOCUMENT_PROFESSIONAL_GUARANTOR_NATURAL_PERSON(DocumentProfessionalGuarantorNaturalPerson.class.getName()),
    DOCUMENT_FINANCIAL_GUARANTOR_NATURAL_PERSON(DocumentFinancialGuarantorNaturalPerson.class.getName()),
    DOCUMENT_TAX_GUARANTOR_NATURAL_PERSON(DocumentTaxGuarantorNaturalPerson.class.getName()),
    DOCUMENT_IDENTIFICATION_GUARANTOR_ORGANISM(DocumentIdentificationGuarantorOrganism.class.getName()),
    DOCUMENT_IDENTIFICATION_GUARANTOR_LEGAL_PERSON(DocumentIdentificationGuarantorLegalPerson.class.getName()),
    DOCUMENT_IDENTIFICATION_REPRESENTANT_GUARANTOR_LEGAL_PERSON(DocumentIdentificationRepresentanGuarantorLegalPerson.class.getName()),
    GUARANTOR_TYPE(GuarantorType.class.getName()),
    NAME_GUARANTOR_LEGAL_PERSON(NameGuarantorLegalPerson.class.getName());
    private final String label;

    StepRegister(String label) {
        this.label = label;
    }
}
