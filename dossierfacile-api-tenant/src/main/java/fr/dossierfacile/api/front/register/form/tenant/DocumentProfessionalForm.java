package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.Extension;
import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import fr.dossierfacile.api.front.validator.anotation.tenant.NumberOfDocument;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

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


@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentProfessionalForm {
    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {CDI, CDI_TRIAL, CDD, ALTERNATION, INTERNSHIP, STUDENT, PUBLIC, CTT, RETIRED, UNEMPLOYED, INDEPENDENT, OTHER})
    private DocumentSubCategory typeDocumentProfessional;

    @NumberOfDocument(max = 15, documentCategory = DocumentCategory.PROFESSIONAL)
    @SizeFile(max = 5, typeDocumentValidation = TypeDocumentValidation.PER_DOCUMENT)
    private List<@Extension MultipartFile> documents = new ArrayList<>();
}
