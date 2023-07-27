package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.residency.NumberOfDocumentResidencyGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST;
import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST_ORGANISM;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OWNER;
import static fr.dossierfacile.common.enums.DocumentSubCategory.SHORT_TERM_RENTAL;
import static fr.dossierfacile.common.enums.DocumentSubCategory.TENANT;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentResidencyGuarantorNaturalPerson
@NumberOfPages(category = DocumentCategory.RESIDENCY, max = 20)
public class DocumentResidencyGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {TENANT, OWNER, GUEST, GUEST_ORGANISM, SHORT_TERM_RENTAL})
    private DocumentSubCategory typeDocumentResidency;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;
}
