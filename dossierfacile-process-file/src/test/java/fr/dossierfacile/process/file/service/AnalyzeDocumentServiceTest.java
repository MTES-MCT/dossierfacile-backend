package fr.dossierfacile.process.file.service;

import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisStatus;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.DocumentRuleLevel;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedList;

class AnalyzeDocumentServiceTest {

    private AnalyzeDocumentService service;
    private Method method;

    @BeforeEach
    void setUp() throws Exception {
        // Le constructeur prend 4 dépendances mais la méthode testée n'en a pas besoin
        service = new AnalyzeDocumentService(null, null, null, null);
        method = AnalyzeDocumentService.class.getDeclaredMethod("computeDocumentAnalysisReportStatus", DocumentAnalysisReport.class);
        method.setAccessible(true);
    }

    private DocumentAnalysisReport emptyReport() {
        return DocumentAnalysisReport.builder()
                .failedRules(new LinkedList<>())
                .passedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    @Test
    void when_initial_status_is_undefined_then_do_nothing() throws Exception {
        var report = emptyReport();
        // Ajoute des règles critiques mais status=UNDEFINED doit court-circuiter
        report.getFailedRules().add(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_BAD_CLASSIFICATION));
        report.setAnalysisStatus(DocumentAnalysisStatus.UNDEFINED);

        method.invoke(service, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.UNDEFINED);
    }

    @Test
    void when_critical_failed_rule_then_status_denied() throws Exception {
        var report = emptyReport();
        report.setAnalysisStatus(DocumentAnalysisStatus.CHECKED);
        report.getFailedRules().add(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_TAX_BAD_CLASSIFICATION)); // CRITICAL

        method.invoke(service, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.DENIED);
    }

    @Test
    void when_inconclusive_and_no_critical_fail_then_status_undefined() throws Exception {
        var report = emptyReport();
        report.setAnalysisStatus(null);
        // échec non critique (WARN) pour vérifier que seul CRITICAL déclenche DENIED
        report.getFailedRules().add(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_BLURRY_FILE)); // WARN
        report.getInconclusiveRules().add(DocumentAnalysisRule.documentInconclusiveRuleFrom(DocumentRule.R_BLURRY_FILE_BLANK));

        method.invoke(service, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.UNDEFINED);
    }

    @Test
    void when_no_critical_fail_and_no_inconclusive_then_status_checked() throws Exception {
        var report = emptyReport();
        report.setAnalysisStatus(null);
        // seulement des règles PASSÉES ou rien dans failed/inconclusive
        report.getPassedRules().add(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_PARSE));

        method.invoke(service, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }

    @Test
    void when_blurry_fail_and_other_pass_then_status_checked() throws Exception {
        var report = emptyReport();
        report.setAnalysisStatus(null);
        // échec non critique (WARN) pour vérifier que seul CRITICAL déclenche DENIED
        report.getFailedRules().add(DocumentAnalysisRule.documentFailedRuleFrom(DocumentRule.R_BLURRY_FILE)); // WARN
        report.getPassedRules().add(DocumentAnalysisRule.documentPassedRuleFrom(DocumentRule.R_TAX_NAMES));

        method.invoke(service, report);

        Assertions.assertThat(report.getAnalysisStatus()).isEqualTo(DocumentAnalysisStatus.CHECKED);
    }
}

