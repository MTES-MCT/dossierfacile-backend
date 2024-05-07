package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.annotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.annotation.LengthOfText;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.tax.NumberOfDocumentTaxGuarantorNaturalPerson;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.tax.OtherTaxCustomTextGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentSubCategory.LESS_THAN_YEAR;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_TAX;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentTaxGuarantorNaturalPerson
@OtherTaxCustomTextGuarantorNaturalPerson
@NumberOfPages(category = DocumentCategory.TAX, max = 10)
public class DocumentTaxGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {MY_NAME, LESS_THAN_YEAR, OTHER_TAX})
    private DocumentSubCategory typeDocumentTax;

    @NotNull
    private Boolean noDocument;

    @LengthOfText(max = 1355)
    private String customText;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;

    private Boolean avisDetected;
}
