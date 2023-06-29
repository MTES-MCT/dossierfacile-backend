package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.identification.NumberOfDocumentIdentificationGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static fr.dossierfacile.common.enums.DocumentSubCategory.DRIVERS_LICENSE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.FRENCH_IDENTITY_CARD;
import static fr.dossierfacile.common.enums.DocumentSubCategory.FRENCH_PASSPORT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.FRENCH_RESIDENCE_PERMIT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_IDENTIFICATION;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentIdentificationGuarantorNaturalPerson
@NumberOfPages(category = DocumentCategory.IDENTIFICATION, max = 5)
public class DocumentIdentificationGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {FRENCH_IDENTITY_CARD, FRENCH_PASSPORT, FRENCH_RESIDENCE_PERMIT, DRIVERS_LICENSE, OTHER_IDENTIFICATION})
    private DocumentSubCategory typeDocumentIdentification;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;

}
