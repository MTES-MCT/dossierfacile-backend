package fr.dossierfacile.process.file.service.documentrules;

import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.RentalReceiptFile;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentCategoryStep;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.LinkedList;
import java.util.List;

public class RentalReceiptRulesValidationServiceTest {

    private final RentalReceiptRulesValidationService service = new RentalReceiptRulesValidationService();

    private File receipt(int pages, YearMonth period, String fullName) {
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .parsedFile(RentalReceiptFile.builder()
                        .tenantFullName(fullName)
                        .period(period)
                        .build())
                .build();
        return File.builder()
                .numberOfPages(pages)
                .parsedFileAnalysis(pfa)
                .build();
    }

    private File receiptWithoutParsed(int pages) {
        return File.builder().numberOfPages(pages).build();
    }

    private Document baseDoc(List<File> files) {
        return Document.builder()
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(files)
                .build();
    }

    private DocumentAnalysisReport emptyReport(Document d) {
        return DocumentAnalysisReport.builder()
                .document(d)
                .failedRules(new LinkedList<>())
                .passedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    private Tenant buildTenant(String lastName, String firstName) {
        ApartmentSharing sharing = ApartmentSharing.builder().applicationType(ApplicationType.ALONE).build();
        Tenant tenant = Tenant.builder()
                .lastName(lastName)
                .firstName(firstName)
                .apartmentSharing(sharing)
                .build();
        sharing.setTenants(List.of(tenant));
        return tenant;
    }

    @Test
    void shouldBeApplied_true() {
        Document doc = baseDoc(List.of(receipt(1, YearMonth.now(), "DUPONT JEAN")));
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_wrong_category() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(List.of(receipt(1, YearMonth.now(), "DUPONT JEAN")))
                .build();
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void shouldBeApplied_false_empty_files() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.RESIDENCY)
                .documentSubCategory(DocumentSubCategory.TENANT)
                .files(List.of())
                .build();
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void process_all_rules_pass_when_enough_pages_parsed_and_valid() {
        // 3 récépissés consécutifs, parsing OK, nom OK, mois OK => toutes les règles passent
        Document doc = baseDoc(List.of(
                receipt(1, YearMonth.now().minusMonths(1), "DUPONT JEAN"),
                receipt(1, YearMonth.now().minusMonths(2), "DUPONT JEAN"),
                receipt(1, YearMonth.now().minusMonths(3), "DUPONT JEAN")
        ));
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS,
                        DocumentRule.R_RENT_RECEIPT_PARSED,
                        DocumentRule.R_RENT_RECEIPT_NAME,
                        DocumentRule.R_RENT_RECEIPT_MONTHS
                );
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_inconclusive_on_parsed_rule_blocks_following_rules() {
        // Missing parsed for one file => PARSED inconclusive (blocking & non PASSED) => stop après PARSED
        Document doc = baseDoc(List.of(
                receipt(2, YearMonth.now().minusMonths(1), "DUPONT JEAN"),
                receiptWithoutParsed(1)
        ));
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS);
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_RENT_RECEIPT_PARSED);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    void process_nb_documents_fail_then_other_rules_evaluated() {
        // Total pages < 3 => NB_DOCUMENTS failed (non blocking), PARSED passed (blocking mais continue car PASSED), NAME passed, MONTHS failed (pas assez de mois)
        Document doc = baseDoc(List.of(
                receipt(1, YearMonth.now().minusMonths(1), "DUPONT JEAN"),
                receipt(1, YearMonth.now().minusMonths(2), "DUPONT JEAN")
        ));
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactlyInAnyOrder(
                        DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS,
                        DocumentRule.R_RENT_RECEIPT_MONTHS
                );
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_RENT_RECEIPT_PARSED,
                        DocumentRule.R_RENT_RECEIPT_NAME
                );
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    void process_nb_documents_ignored_when_tenant_proof_step() {
        // Step TENANT_PROOF : NB_DOCUMENTS auto OK. On fournit 3 reçus pour satisfaire MONTHS.
        Document doc = baseDoc(List.of(
                receipt(1, YearMonth.now().minusMonths(1), "DUPONT JEAN"),
                receipt(1, YearMonth.now().minusMonths(2), "DUPONT JEAN"),
                receipt(1, YearMonth.now().minusMonths(3), "DUPONT JEAN")
        ));
        doc.setDocumentCategoryStep(DocumentCategoryStep.TENANT_PROOF);
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS,
                        DocumentRule.R_RENT_RECEIPT_PARSED,
                        DocumentRule.R_RENT_RECEIPT_NAME,
                        DocumentRule.R_RENT_RECEIPT_MONTHS
                );
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    void process_name_and_months_fail_after_parsed_pass() {
        // Nom incorrect + mois non valides -> échecs NAME et MONTHS après NB_DOCUMENTS & PARSED passés
        Document doc = baseDoc(List.of(
                receipt(1, YearMonth.now().minusMonths(1), "AUTRE NOM"),
                receipt(1, YearMonth.now().minusMonths(6), "AUTRE NOM"),
                receipt(1, YearMonth.now().minusMonths(9), "AUTRE NOM")
        ));
        doc.setTenant(buildTenant("DUPONT", "JEAN"));
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);

        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_RENT_RECEIPT_NB_DOCUMENTS,
                        DocumentRule.R_RENT_RECEIPT_PARSED
                );
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactlyInAnyOrder(
                        DocumentRule.R_RENT_RECEIPT_NAME,
                        DocumentRule.R_RENT_RECEIPT_MONTHS
                );
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }
}
