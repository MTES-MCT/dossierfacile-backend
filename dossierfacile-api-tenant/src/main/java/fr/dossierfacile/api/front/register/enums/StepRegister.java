package fr.dossierfacile.api.front.register.enums;

import fr.dossierfacile.api.front.register.guarantor.legal_person.DocumentIdentificationGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.legal_person.DocumentIdentificationRepresentanGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.legal_person.NameGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.legal_person.NameRepresentantGuarantorLegalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentFinancialGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentIdentificationGuarantorNaturalPersonFile;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentProfessionalGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentResidencyGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.DocumentTaxGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.natural_person.NameGuarantorNaturalPerson;
import fr.dossierfacile.api.front.register.guarantor.organism.DocumentGuaranteeProviderCertificate;
import fr.dossierfacile.api.front.register.guarantor.organism.DocumentIdentificationGuarantorOrganism;
import fr.dossierfacile.api.front.register.tenant.AccountApiPartner;
import fr.dossierfacile.api.front.register.tenant.Application;
import fr.dossierfacile.api.front.register.tenant.DocumentFinancial;
import fr.dossierfacile.api.front.register.tenant.DocumentIdentification;
import fr.dossierfacile.api.front.register.tenant.DocumentProfessional;
import fr.dossierfacile.api.front.register.tenant.DocumentResidency;
import fr.dossierfacile.api.front.register.tenant.DocumentTax;
import fr.dossierfacile.api.front.register.tenant.GuarantorType;
import fr.dossierfacile.api.front.register.tenant.HonorDeclaration;
import fr.dossierfacile.api.front.register.tenant.Names;
import lombok.Getter;

@Getter
public enum StepRegister {
    ACCOUNT_PARTNER_API(AccountApiPartner.class),
    NAMES(Names.class),
    APPLICATION(Application.class),
    HONOR_DECLARATION(HonorDeclaration.class),
    DOCUMENT_IDENTIFICATION(DocumentIdentification.class),
    DOCUMENT_RESIDENCY(DocumentResidency.class),
    DOCUMENT_PROFESSIONAL(DocumentProfessional.class),
    DOCUMENT_FINANCIAL(DocumentFinancial.class),
    DOCUMENT_TAX(DocumentTax.class),
    NAME_GUARANTOR_NATURAL_PERSON(NameGuarantorNaturalPerson.class),
    DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON_FILE(DocumentIdentificationGuarantorNaturalPersonFile.class),
    DOCUMENT_IDENTIFICATION_GUARANTOR_NATURAL_PERSON(DocumentIdentificationGuarantorNaturalPerson.class),
    DOCUMENT_RESIDENCY_GUARANTOR_NATURAL_PERSON(DocumentResidencyGuarantorNaturalPerson.class),
    DOCUMENT_PROFESSIONAL_GUARANTOR_NATURAL_PERSON(DocumentProfessionalGuarantorNaturalPerson.class),
    DOCUMENT_FINANCIAL_GUARANTOR_NATURAL_PERSON(DocumentFinancialGuarantorNaturalPerson.class),
    DOCUMENT_TAX_GUARANTOR_NATURAL_PERSON(DocumentTaxGuarantorNaturalPerson.class),
    DOCUMENT_IDENTIFICATION_GUARANTOR_ORGANISM(DocumentIdentificationGuarantorOrganism.class),
    DOCUMENT_GUARANTEE_PROVIDER_CERTIFICATE(DocumentGuaranteeProviderCertificate.class),
    DOCUMENT_IDENTIFICATION_GUARANTOR_LEGAL_PERSON(DocumentIdentificationGuarantorLegalPerson.class),
    DOCUMENT_IDENTIFICATION_REPRESENTANT_GUARANTOR_LEGAL_PERSON(DocumentIdentificationRepresentanGuarantorLegalPerson.class),
    GUARANTOR_TYPE(GuarantorType.class),
    NAME_GUARANTOR_LEGAL_PERSON(NameGuarantorLegalPerson.class),
    NAME_IDENTIFICATION_REPRESENTANT_GUARANTOR_LEGAL_PERSON(NameRepresentantGuarantorLegalPerson.class);

    private final Class<?> clazz;

    StepRegister(Class<?> clazz) {
        this.clazz = clazz;
    }

    public String getLabel() {
        return clazz.getName();
    }

}
