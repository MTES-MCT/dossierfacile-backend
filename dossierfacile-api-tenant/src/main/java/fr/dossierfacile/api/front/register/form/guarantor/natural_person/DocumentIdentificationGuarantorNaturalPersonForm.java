package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.annotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.annotation.guarantor.natural_person.identification.NumberOfDocumentIdentificationGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentSubCategory.DRIVERS_LICENSE;
import static fr.dossierfacile.common.enums.DocumentSubCategory.FRANCE_IDENTITE;
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
    @DocumentSubcategorySubset(anyOf = {
            FRENCH_IDENTITY_CARD, FRENCH_PASSPORT, FRENCH_RESIDENCE_PERMIT, DRIVERS_LICENSE,
            FRANCE_IDENTITE, OTHER_IDENTIFICATION
    })
    private DocumentSubCategory typeDocumentIdentification;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;

}
