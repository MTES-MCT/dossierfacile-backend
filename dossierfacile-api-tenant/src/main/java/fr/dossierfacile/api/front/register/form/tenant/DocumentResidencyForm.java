package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.anotation.tenant.residency.NumberOfDocumentResidency;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST;
import static fr.dossierfacile.common.enums.DocumentSubCategory.GUEST_PARENTS;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OWNER;
import static fr.dossierfacile.common.enums.DocumentSubCategory.TENANT;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentResidency
@NumberOfPages(category = DocumentCategory.RESIDENCY, max = 20)
public class DocumentResidencyForm extends DocumentForm {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {TENANT, OWNER, GUEST, GUEST_PARENTS})
    private DocumentSubCategory typeDocumentResidency;
}
