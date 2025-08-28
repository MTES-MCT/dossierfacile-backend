package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.annotation.DocumentCategoryStepSubset;
import fr.dossierfacile.api.front.validator.annotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.annotation.LengthOfText;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.annotation.tenant.tax.NumberOfDocumentTax;
import fr.dossierfacile.api.front.validator.annotation.tenant.tax.OtherTaxCustomText;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentCategoryStep.TAX_FOREIGN_NOTICE;
import static fr.dossierfacile.common.enums.DocumentCategoryStep.TAX_FRENCH_NOTICE;
import static fr.dossierfacile.common.enums.DocumentCategoryStep.TAX_NOT_RECEIVED;
import static fr.dossierfacile.common.enums.DocumentCategoryStep.TAX_NO_DECLARATION;
import static fr.dossierfacile.common.enums.DocumentSubCategory.LESS_THAN_YEAR;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_PARENTS;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_TAX;

import javax.annotation.Nullable;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentTax
@OtherTaxCustomText
@NumberOfPages(category = DocumentCategory.TAX, max = 10)
public class DocumentTaxForm extends DocumentForm {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {MY_NAME, MY_PARENTS, LESS_THAN_YEAR, OTHER_TAX})
    private DocumentSubCategory typeDocumentTax;

    @Nullable
    @DocumentCategoryStepSubset(anyOf = {TAX_FOREIGN_NOTICE, TAX_FRENCH_NOTICE, TAX_NOT_RECEIVED, TAX_NO_DECLARATION})
    private DocumentCategoryStep categoryStep;

    @NotNull
    private Boolean noDocument;

    @LengthOfText(max = 1355)
    private String customText;

    private Boolean avisDetected;
}
