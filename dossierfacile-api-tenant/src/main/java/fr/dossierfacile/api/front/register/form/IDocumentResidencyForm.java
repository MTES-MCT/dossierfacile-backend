package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDocumentResidencyForm {
    DocumentSubCategory getTypeDocumentResidency();

    DocumentCategoryStep getCategoryStep();

    String getCustomText();

    List<MultipartFile> getDocuments();
}
