package fr.dossierfacile.api.front.validator.guarantor.natural_person.tax;

import fr.dossierfacile.api.front.register.form.guarantor.natural_person.DocumentTaxGuarantorNaturalPersonForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.NumberOfDocumentTaxValidator;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.tax.NumberOfDocumentTaxGuarantorNaturalPerson;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.springframework.stereotype.Component;

@Component
public class NumberOfDocumentTaxGuarantorNaturalPersonValidator extends NumberOfDocumentTaxValidator<NumberOfDocumentTaxGuarantorNaturalPerson, DocumentTaxGuarantorNaturalPersonForm> {

    public NumberOfDocumentTaxGuarantorNaturalPersonValidator(FileRepository fileRepository) {
        super(fileRepository);
    }

    @Override
    protected long getOldCount(DocumentTaxGuarantorNaturalPersonForm documentTaxForm) {
        Tenant tenant = getTenant(documentTaxForm);
        return fileRepository.countFileByDocumentCategoryGuarantorIdTypeGuarantorTenant(
                DocumentCategory.TAX,
                documentTaxForm.getGuarantorId(),
                TypeGuarantor.NATURAL_PERSON,
                tenant
        );
    }

    @Override
    protected long getNewCount(DocumentTaxGuarantorNaturalPersonForm documentTaxForm) {
        return documentTaxForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();
    }

    @Override
    protected DocumentSubCategory getTypeDocumentTax(DocumentTaxGuarantorNaturalPersonForm documentTaxForm) {
        return documentTaxForm.getTypeDocumentTax();
    }

    @Override
    protected boolean getNoDocument(DocumentTaxGuarantorNaturalPersonForm documentTaxForm) {
        return documentTaxForm.getNoDocument();
    }
}
