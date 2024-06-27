package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.annotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.annotation.LengthOfText;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.residency.CustomTextResidencyGuarantorNaturalPerson;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.residency.NumberOfDocumentResidencyGuarantorNaturalPerson;
import fr.dossierfacile.common.config.ApiVersion;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentSubCategory.*;
import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST_COMPANY;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@CustomTextResidencyGuarantorNaturalPerson
@NumberOfDocumentResidencyGuarantorNaturalPerson
@NumberOfPages(category = DocumentCategory.RESIDENCY, max = 20)
public class DocumentResidencyGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {TENANT, OWNER, GUEST, GUEST_PARENTS, GUEST_ORGANISM, SHORT_TERM_RENTAL, OTHER_RESIDENCY}, groups = ApiVersion.V3.class)
    @DocumentSubcategorySubset(anyOf =
            {TENANT, OWNER, GUEST, GUEST_COMPANY, GUEST_ORGANISM, SHORT_TERM_RENTAL, OTHER_RESIDENCY}, groups = ApiVersion.V4.class)
    private DocumentSubCategory typeDocumentResidency;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;

    @LengthOfText(max = 2000)
    private String customText;

}
