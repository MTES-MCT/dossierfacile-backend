package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;

public interface IDocumentFinancialForm {
    DocumentSubCategory getTypeDocumentFinancial();

    Integer getMonthlySum();

    DocumentCategoryStep getCategoryStep();

    void setCategoryStep(DocumentCategoryStep categoryStep);
}
