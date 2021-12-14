package fr.dossierfacile.api.front.register.form.guarantor.natural_person;

import fr.dossierfacile.api.front.register.form.guarantor.DocumentGuarantorFormAbstract;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.guarantor.natural_person.professional.NumberOfDocumentProfessionalGuarantorNaturalPerson;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TypeGuarantor;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static fr.dossierfacile.common.enums.DocumentSubCategory.ALTERNATION;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CDD;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CDI;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CDI_TRIAL;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CTT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.INDEPENDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.INTERNSHIP;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER;
import static fr.dossierfacile.common.enums.DocumentSubCategory.PUBLIC;
import static fr.dossierfacile.common.enums.DocumentSubCategory.RETIRED;
import static fr.dossierfacile.common.enums.DocumentSubCategory.STUDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.UNEMPLOYED;


@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentProfessionalGuarantorNaturalPerson
@NumberOfPages(category = DocumentCategory.PROFESSIONAL, max = 50)
public class DocumentProfessionalGuarantorNaturalPersonForm extends DocumentGuarantorFormAbstract {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {CDI, CDI_TRIAL, CDD, ALTERNATION, INTERNSHIP, STUDENT, PUBLIC, CTT, RETIRED, UNEMPLOYED, INDEPENDENT, OTHER})
    private DocumentSubCategory typeDocumentProfessional;

    private TypeGuarantor typeGuarantor = TypeGuarantor.NATURAL_PERSON;
}
