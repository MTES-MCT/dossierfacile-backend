package fr.dossierfacile.api.front.register.form;

import fr.dossierfacile.common.enums.DocumentSubCategory;

public interface IDocumentFinancialForm {
    DocumentSubCategory getTypeDocumentFinancial();

    Integer getMonthlySum();
}
