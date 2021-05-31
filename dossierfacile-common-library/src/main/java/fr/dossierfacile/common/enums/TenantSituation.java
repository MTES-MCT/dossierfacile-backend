package fr.dossierfacile.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TenantSituation {
    CDI("tenant.form.situation.cdi", Constants.OPTION3, Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, Constants.TENANT_PDF_FILE3_CDI, Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    CDI_TRIAL("tenant.form.situation.cdi_trial", Constants.OPTION3, Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, Constants.TENANT_PDF_FILE3_CDI, Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    CDD("tenant.form.situation.cdd", Constants.OPTION3, Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, Constants.TENANT_PDF_FILE3_CDI, Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    INTERNSHIP("tenant.form.situation.internship", "bo.tenant.custom.email.option3.internship", Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.internship", Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    ALTERNATION("tenant.form.situation.alternation", "bo.tenant.custom.email.option3.alternation", Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.alternation", Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    STUDENT("tenant.form.situation.student", "bo.tenant.custom.email.option3.student", "bo.tenant.custom.email.option5.student", Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.student", "tenant.pdf.file5.student", Constants.TENANT_PDF_FILE4),
    PUBLIC("tenant.form.situation.public", "bo.tenant.custom.email.option3.public", Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.public", Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    CTT("tenant.form.situation.ctt", Constants.OPTION3, Constants.OPTION5, Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, Constants.TENANT_PDF_FILE3_CDI, Constants.TENANT_PDF_FILE5_CDI, Constants.TENANT_PDF_FILE4),
    RETIRED("tenant.form.situation.retired", "bo.tenant.custom.email.option3.retired", "bo.tenant.custom.email.option5.retired", Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.retire", "tenant.pdf.file5.retire", Constants.TENANT_PDF_FILE4),
    UNEMPLOYED("tenant.form.situation.unemployed", "bo.tenant.custom.email.option3.unemployed", "bo.tenant.custom.email.option5.unemployed", Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.unemployed", "tenant.pdf.file5.unemployed", Constants.TENANT_PDF_FILE4),
    INDEPENDENT("tenant.form.situation.independent", "bo.tenant.custom.email.option3.independent", "bo.tenant.custom.email.option5.independent", Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.independent", "tenant.pdf.file5.independent", Constants.TENANT_PDF_FILE4),
    OTHER("tenant.form.situation.other", "bo.tenant.custom.email.option3.other", "bo.tenant.custom.email.option5.other", Constants.TENANT_PDF_FILE1, Constants.TENANT_PDF_FILE2, "tenant.pdf.file3.other", "tenant.pdf.file5.other", Constants.TENANT_PDF_FILE4),
    UNDEFINED;
    String label;
    String messageFile3;
    String messageFile5;
    String namePdfFile1;
    String namePdfFile2;
    String namePdfFile3;
    String namePdfFile4;
    String namePdfFile5;

    public String getText(int number) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method get = fr.dossierfacile.common.enums.TenantSituation.class.getDeclaredMethod("getNamePdfFile" + number);
        return (String) get.invoke(this);
    }

    private static class Constants {
        private static final String OPTION3 = "bo.tenant.custom.email.option3";
        private static final String OPTION5 = "bo.tenant.custom.email.option5";
        private static final String TENANT_PDF_FILE1 = "tenant.pdf.file1";
        private static final String TENANT_PDF_FILE2 = "tenant.pdf.file2";
        private static final String TENANT_PDF_FILE4 = "tenant.pdf.file4";
        private static final String TENANT_PDF_FILE3_CDI = "tenant.pdf.file3.cdi";
        private static final String TENANT_PDF_FILE5_CDI = "tenant.pdf.file5.cdi";
    }
}
