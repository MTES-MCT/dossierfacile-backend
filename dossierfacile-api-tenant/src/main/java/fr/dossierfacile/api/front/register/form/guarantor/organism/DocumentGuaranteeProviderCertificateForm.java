package fr.dossierfacile.api.front.register.form.guarantor.organism;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.annotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_GUARANTEE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.VISALE;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfPages(category = DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE, max = 10)
public class DocumentGuaranteeProviderCertificateForm extends DocumentGuarantorFormAbstract {

    private TypeGuarantor typeGuarantor = TypeGuarantor.ORGANISM;

    private DocumentCategory documentCategory = DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE;

    @NotNull
    @DocumentSubcategorySubset(anyOf = {
            VISALE, OTHER_GUARANTEE
    })
    private DocumentSubCategory typeDocumentCertificate;

}
