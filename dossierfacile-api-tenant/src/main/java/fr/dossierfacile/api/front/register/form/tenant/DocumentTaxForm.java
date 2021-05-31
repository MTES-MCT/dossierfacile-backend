package fr.dossierfacile.api.front.register.form.tenant;

import fr.dossierfacile.api.front.validator.anotation.DocumentSubcategorySubset;
import fr.dossierfacile.api.front.validator.anotation.Extension;
import fr.dossierfacile.api.front.validator.anotation.LengthOfText;
import fr.dossierfacile.api.front.validator.anotation.SizeFile;
import fr.dossierfacile.api.front.validator.anotation.tenant.tax.MyNameAcceptVerification;
import fr.dossierfacile.api.front.validator.anotation.tenant.tax.NumberOfDocumentTax;
import fr.dossierfacile.api.front.validator.anotation.tenant.tax.OtherTaxCustomText;
import fr.dossierfacile.api.front.validator.enums.TypeDocumentValidation;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

import static fr.dossierfacile.common.enums.DocumentSubCategory.LESS_THAN_YEAR;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_NAME;
import static fr.dossierfacile.common.enums.DocumentSubCategory.MY_PARENTS;
import static fr.dossierfacile.common.enums.DocumentSubCategory.OTHER_TAX;

@Data
@AllArgsConstructor
@NoArgsConstructor
@NumberOfDocumentTax
@OtherTaxCustomText
@MyNameAcceptVerification
public class DocumentTaxForm {
    @NotNull
    @DocumentSubcategorySubset(anyOf =
            {MY_NAME, MY_PARENTS, LESS_THAN_YEAR, OTHER_TAX})
    private DocumentSubCategory typeDocumentTax;

    @NotNull
    private Boolean noDocument;

    @LengthOfText(max = 1355)
    private String customText;

    private Boolean acceptVerification;

    @SizeFile(max = 5, typeDocumentValidation = TypeDocumentValidation.PER_DOCUMENT)
    private List<@Extension MultipartFile> documents = new ArrayList<>();
}
