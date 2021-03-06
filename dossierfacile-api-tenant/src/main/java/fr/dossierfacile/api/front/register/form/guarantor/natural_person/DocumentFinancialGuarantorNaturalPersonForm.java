package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.LengthOfText;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NoDocumentCustomTextFinancialGuarantorNaturalPerson;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NumberOfDocumentFinancialGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static fr.dossierfacile.common.enums.DocumentSubCategory.PENSION;
import static fr.dossierfacile.common.enums.DocumentSubCategory.RENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SCHOLARSHIP;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SOCIAL_SERVICE;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NoDocumentCustomTextFinancialGuarantorNaturalPerson
@NumberOfDocumentFinancialGuarantorNaturalPerson
@NumberOfPages(category = DocumentCategory.FINANCIAL, max = 50)
public class DocumentFinancialGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract {

    private Long documentId;

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {SALARY, SOCIAL_SERVICE, RENT, PENSION, SCHOLARSHIP})
    private DocumentSubCategory typeDocumentFinancial;

    private Integer monthlySum;

    @NotNull
    private Boolean noDocument;

    @LengthOfText(max = 2000)
    private String customText;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;
}
