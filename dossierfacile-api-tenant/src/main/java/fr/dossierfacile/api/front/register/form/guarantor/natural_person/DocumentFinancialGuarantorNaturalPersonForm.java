package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.Extension;
import fr.dossierfacile.api.front.validator.anotation.LengthOfText;
import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.ExistGuarantor;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NoDocumentCustomTextFinancialGuarantorNaturalPerson;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.financial.NumberOfDocumentFinancialGuarantorNaturalPerson;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.PENSION;
import static fr.dossierfacile.common.enums.DocumentSubCategory.RENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SALARY;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SCHOLARSHIP;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SOCIAL_SERVICE;

@Data
@AllArgsConstructor
@NoArgsConstructor
@NoDocumentCustomTextFinancialGuarantorNaturalPerson
@NumberOfDocumentFinancialGuarantorNaturalPerson
public class DocumentFinancialGuarantorNaturalPersonForm {

    private Long documentId;

    @NotNull
    @ExistGuarantor(typeGuarantor = TypeGuarantor.NATURAL_PERSON)
    private Long guarantorId;

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {SALARY, SOCIAL_SERVICE, RENT, PENSION, SCHOLARSHIP})
    private DocumentSubCategory typeDocumentFinancial;

    private Integer monthlySum;

    @NotNull
    private Boolean noDocument;

    @LengthOfText(max = 1355)
    private String customText;

    @SizeFile(max = 5, typeDocumentValidation = TypeDocumentValidation.PER_DOCUMENT)
    private List<@Extension MultipartFile> documents = new ArrayList<>();
}
