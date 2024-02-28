package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.LengthOfText;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.tenant.financial.NoDocumentCustomTextFinancial;
import fr.dossierfacile.api.front.validator.anotation.tenant.financial.NumberOfDocumentFinancial;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentSubCategory.NO_INCOME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.PENSION;
import static fr.dossierfacile.common.enums.DocumentSubCategory.RENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SCHOLARSHIP;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SOCIAL_SERVICE;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NoDocumentCustomTextFinancial
@NumberOfDocumentFinancial
@NumberOfPages(category = DocumentCategory.FINANCIAL, max = 50)
public class DocumentFinancialForm extends DocumentForm {

    @Parameter(description = "Identifiant du document est nécessaire poour les mises à jour")
    private Long id;

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {SALARY, SOCIAL_SERVICE, RENT, PENSION, SCHOLARSHIP, NO_INCOME})
    private DocumentSubCategory typeDocumentFinancial;

    private Integer monthlySum;

    @NotNull
    private Boolean noDocument;

    @LengthOfText(max = 2000)
    private String customText;
}
