package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.register.form.DocumentForm;
import fr.dossierfacile.api.front.validator.annotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.annotation.LengthOfText;
import fr.dossierfacile.api.front.validator.annotation.NumberOfPages;
import fr.dossierfacile.api.front.validator.annotation.tenant.residency.CustomTextResidency;
import fr.dossierfacile.api.front.validator.annotation.tenant.residency.NumberOfDocumentResidency;
import fr.dossierfacile.common.config.ApiVersion;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import static fr.dossierfacile.common.enums.DocumentSubCategory.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@CustomTextResidency
@NumberOfDocumentResidency
@NumberOfPages(category = DocumentCategory.RESIDENCY, max = 20)
public class DocumentResidencyForm extends DocumentForm {

    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {TENANT, OWNER, GUEST, GUEST_COMPANY, GUEST_ORGANISM, SHORT_TERM_RENTAL, OTHER_RESIDENCY}, groups = ApiVersion.V4.class)
    private DocumentSubCategory typeDocumentResidency;

    @LengthOfText(max = 2000)
    private String customText;

}
