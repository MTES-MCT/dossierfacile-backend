package fr.dossierfacile.api.front.validator.tenant.tax;

import fr.dossierfacile.api.front.register.form.tenant.DocumentTaxForm;
import fr.dossierfacile.api.front.repository.FileRepository;
import fr.dossierfacile.api.front.validator.NumberOfDocumentTaxValidator;
import fr.dossierfacile.api.front.validator.annotation.tenant.tax.NumberOfDocumentTax;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
public class TenantNumberOfDocumentTaxValidator extends NumberOfDocumentTaxValidator<NumberOfDocumentTax, DocumentTaxForm> {

    public TenantNumberOfDocumentTaxValidator(FileRepository fileRepository) {
        super(fileRepository);
    }

    @Override
    protected long getOldCount(DocumentTaxForm documentTaxForm) {
        Tenant tenant = getTenant(documentTaxForm);
        return fileRepository.countFileByDocumentCategoryTenant(DocumentCategory.TAX, tenant);
    }

    @Override
    protected long getNewCount(DocumentTaxForm documentTaxForm) {
        return documentTaxForm.getDocuments().stream().filter(f -> !f.isEmpty()).count();
    }

    @Override
    protected DocumentSubCategory getTypeDocumentTax(DocumentTaxForm documentTaxForm) {
        return documentTaxForm.getTypeDocumentTax();
    }

    @Override
    protected boolean getNoDocument(DocumentTaxForm documentTaxForm) {
        return documentTaxForm.getNoDocument();
    }
}
