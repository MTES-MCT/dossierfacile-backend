package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.tenant.profesional.NumberOfDocumentProfesional;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static fr.dossierfacile.common.enums.DocumentSubCategory.ALTERNATION;
import static fr.dossierfacile.common.enums.DocumentSubCategory.ARTIST;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CDD;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CDI;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CDI_TRIAL;
import static fr.dossierfacile.common.enums.DocumentSubCategory.CTT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.INDEPENDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.INTERMITTENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.INTERNSHIP;
import static fr.dossierfacile.common.enums.DocumentSubCategory.NO_ACTIVITY;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER;
import static fr.dossierfacile.common.enums.DocumentSubCategory.PUBLIC;
import static fr.dossierfacile.common.enums.DocumentSubCategory.RETIRED;
import static fr.dossierfacile.common.enums.DocumentSubCategory.STAY_AT_HOME_PARENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.STUDENT;
import static fr.dossierfacile.common.enums.DocumentSubCategory.UNEMPLOYED;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentProfesional(max = 20)
@NumberOfPages(category = DocumentCategory.PROFESSIONAL, max = 50)
public class DocumentProfessionalForm extends DocumentForm {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {CDI, CDI_TRIAL, CDD, ALTERNATION, INTERNSHIP, STUDENT, PUBLIC, CTT, RETIRED, UNEMPLOYED, INDEPENDENT,
                    INTERMITTENT, STAY_AT_HOME_PARENT, NO_ACTIVITY, ARTIST, OTHER})
    private DocumentSubCategory typeDocumentProfessional;
}
