package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IDocumentFinancialForm {
    DocumentSubCategory getTypeDocumentFinancial();

    Integer getMonthlySum();

    DocumentCategoryStep getCategoryStep();

    void setCategoryStep(DocumentCategoryStep categoryStep);

    Boolean getNoDocument();

    String getCustomText();

    List<MultipartFile> getDocuments();
}
