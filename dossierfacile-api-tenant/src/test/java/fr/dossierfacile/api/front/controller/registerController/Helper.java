package fr.dossierfacile.api.front.controller.registerController;

import fr.dossierfacile.api.front.register.form.tenant.DocumentFinancialForm;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;

public class Helper {

    public static DocumentFinancialForm invalidDocumentBecauseNoDocument() {
        DocumentFinancialForm documentFinancialForm = new DocumentFinancialForm();
        documentFinancialForm.setMonthlySum(1000);
        documentFinancialForm.setCategoryStep(DocumentCategoryStep.SALARY_EMPLOYED_NOT_YET);
        documentFinancialForm.setTypeDocumentFinancial(DocumentSubCategory.SALARY);
        return documentFinancialForm;
    }

    public static DocumentFinancialForm invalidDocumentBecauseWrongType() {
        DocumentFinancialForm documentFinancialForm = new DocumentFinancialForm();
        documentFinancialForm.setMonthlySum(1000);
        documentFinancialForm.setNoDocument(false);
        documentFinancialForm.setCategoryStep(DocumentCategoryStep.SALARY_EMPLOYED_NOT_YET);
        documentFinancialForm.setTypeDocumentFinancial(DocumentSubCategory.MY_NAME);
        return documentFinancialForm;
    }

    public static DocumentFinancialForm invalidDocumentBecauseWrongStep() {
        DocumentFinancialForm documentFinancialForm = new DocumentFinancialForm();
        documentFinancialForm.setMonthlySum(1000);
        documentFinancialForm.setNoDocument(false);
        documentFinancialForm.setCategoryStep(DocumentCategoryStep.PENSION_NO_STATEMENT);
        documentFinancialForm.setTypeDocumentFinancial(DocumentSubCategory.SALARY);
        return documentFinancialForm;
    }

    public static DocumentFinancialForm invalidDocumentBecauseNoStep() {
        DocumentFinancialForm documentFinancialForm = new DocumentFinancialForm();
        documentFinancialForm.setMonthlySum(1000);
        documentFinancialForm.setNoDocument(false);
        documentFinancialForm.setCategoryStep(DocumentCategoryStep.SALARY_EMPLOYED_NOT_YET);
        documentFinancialForm.setTypeDocumentFinancial(DocumentSubCategory.SCHOLARSHIP);
        return documentFinancialForm;
    }

    public static DocumentFinancialForm invalidDocumentBecauseWrongStepPartner() {
        DocumentFinancialForm documentFinancialForm = new DocumentFinancialForm();
        documentFinancialForm.setMonthlySum(1000);
        documentFinancialForm.setNoDocument(true);
        documentFinancialForm.setTenantId(1L);
        documentFinancialForm.setCustomText("test");
        documentFinancialForm.setCategoryStep(DocumentCategoryStep.PENSION_NO_STATEMENT);
        documentFinancialForm.setTypeDocumentFinancial(DocumentSubCategory.SALARY);
        return documentFinancialForm;
    }

    public static DocumentFinancialForm validDocumentForPartner() {
        DocumentFinancialForm documentFinancialForm = new DocumentFinancialForm();
        documentFinancialForm.setMonthlySum(1000);
        documentFinancialForm.setNoDocument(true);
        documentFinancialForm.setTenantId(1L);
        documentFinancialForm.setCustomText("test");
        documentFinancialForm.setTypeDocumentFinancial(DocumentSubCategory.SALARY);
        return documentFinancialForm;
    }


}
