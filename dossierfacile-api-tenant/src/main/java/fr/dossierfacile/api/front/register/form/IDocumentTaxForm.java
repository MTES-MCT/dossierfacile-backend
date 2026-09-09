package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDocumentTaxForm {
    DocumentSubCategory getTypeDocumentTax();

    DocumentCategoryStep getCategoryStep();

    Boolean getNoDocument();

    String getCustomText();

    Boolean getAvisDetected();

    List<MultipartFile> getDocuments();
}
