package fr.dossierfacile.process.file.service.document_rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.dossierfacile.common.entity.*;
import fr.dossierfacile.common.entity.ocr.TaxIncomeLeaf;
import fr.dossierfacile.common.entity.ocr.TaxIncomeMainFile;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.ParsedFileAnalysisStatus;
import fr.dossierfacile.common.enums.ParsedFileClassification;
import fr.dossierfacile.process.file.barcode.twoddoc.parsing.TwoDDocDataType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

class IncomeTaxRulesValidationServiceTest {

    private final IncomeTaxRulesValidationService service = new IncomeTaxRulesValidationService();
    private final ObjectMapper mapper = new ObjectMapper();

    private ObjectNode qrNode(int year, String decl1Nom, String decl2Nom, int rfr) {
        ObjectNode node = mapper.createObjectNode();
        node.put(TwoDDocDataType.ID_47.getLabel(), "NF1");
        node.put(TwoDDocDataType.ID_46.getLabel(), decl1Nom); // declarant1Nom
        node.put(TwoDDocDataType.ID_49.getLabel(), "NF2");
        node.put(TwoDDocDataType.ID_48.getLabel(), decl2Nom == null ? "" : decl2Nom); // declarant2Nom
        node.put(TwoDDocDataType.ID_45.getLabel(), String.valueOf(year)); // annee revenus
        node.put(TwoDDocDataType.ID_43.getLabel(), "2");
        node.put(TwoDDocDataType.ID_4A.getLabel(), "20240101");
        node.put(TwoDDocDataType.ID_41.getLabel(), String.valueOf(rfr));
        node.put(TwoDDocDataType.ID_44.getLabel(), "REF");
        return node;
    }

    private TaxIncomeMainFile parsedMain(int year, String decl1Nom, Integer rfr, boolean withLeaves) {
        List<TaxIncomeLeaf> leaves = withLeaves ? List.of(
                TaxIncomeLeaf.builder().page(1).pageCount(2).build(),
                TaxIncomeLeaf.builder().page(2).pageCount(2).build()
        ) : null;
        return TaxIncomeMainFile.builder()
                .anneeDesRevenus(year)
                .declarant1Nom(decl1Nom)
                .revenuFiscalDeReference(rfr)
                .taxIncomeLeaves(leaves)
                .build();
    }

    private File taxFile(int year, String decl1Nom, String decl2Nom, int rfr, boolean withLeaves) {
        ObjectNode qr = qrNode(year, decl1Nom, decl2Nom, rfr);
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.TWO_D_DOC)
                .documentType(BarCodeDocumentType.TAX_ASSESSMENT)
                .verifiedData(qr)
                .build();
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(parsedMain(year, decl1Nom, rfr, withLeaves))
                .build();
        File f = File.builder().fileAnalysis(bar).parsedFileAnalysis(pfa).build();
        bar.setFile(f);
        pfa.setFile(f);
        return f;
    }

    private File taxFileNoParsed(int year, String decl1Nom, int rfr) {
        ObjectNode qr = qrNode(year, decl1Nom, null, rfr);
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder()
                .barCodeType(BarCodeType.TWO_D_DOC)
                .documentType(BarCodeDocumentType.TAX_ASSESSMENT)
                .verifiedData(qr)
                .build();
        // Pas de parsed ou parsed incomplet -> HasBeenParsed INCONCLUSIVE
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder()
                .analysisStatus(ParsedFileAnalysisStatus.COMPLETED)
                .classification(ParsedFileClassification.TAX_INCOME)
                .parsedFile(TaxIncomeMainFile.builder().declarant1Nom(null).anneeDesRevenus(null).revenuFiscalDeReference(null).build())
                .build();
        File f = File.builder().fileAnalysis(bar).parsedFileAnalysis(pfa).build();
        bar.setFile(f); pfa.setFile(f);
        return f;
    }

    private Document baseDoc(List<File> files) {
        return Document.builder()
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .files(files)
                .build();
    }

    private DocumentAnalysisReport emptyReport(Document doc) {
        return DocumentAnalysisReport.builder()
                .document(doc)
                .passedRules(new LinkedList<>())
                .failedRules(new LinkedList<>())
                .inconclusiveRules(new LinkedList<>())
                .build();
    }

    @Test
    void shouldBeApplied_true() {
        Document doc = baseDoc(List.of(taxFile(LocalDate.now().getYear()-1, "DUPONT JEAN", null, 12345, true)));
        Assertions.assertThat(service.shouldBeApplied(doc)).isTrue();
    }

    @Test
    void shouldBeApplied_false_wrong_category() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.FINANCIAL)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .files(List.of(taxFile(LocalDate.now().getYear()-1, "DUPONT JEAN", null, 12345, true)))
                .build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void shouldBeApplied_false_wrong_subcategory() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.OTHER_TAX)
                .files(List.of(taxFile(LocalDate.now().getYear()-1, "DUPONT JEAN", null, 12345, true)))
                .build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void shouldBeApplied_false_no_files() {
        Document doc = Document.builder()
                .documentCategory(DocumentCategory.TAX)
                .documentSubCategory(DocumentSubCategory.MY_NAME)
                .build();
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    void shouldBeApplied_false_no_file_analysis() {
        // fichier sans fileAnalysis
        File f = File.builder().build();
        Document doc = baseDoc(List.of(f));
        Assertions.assertThat(service.shouldBeApplied(doc)).isFalse();
    }

    @Test
    @DisplayName("Process - tous les règles passent")
    void process_all_rules_pass() {
        int year = LocalDate.now().getYear() - 1;
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").preferredName("MARTIN").build();
        File f = taxFile(year, "DUPONT JEAN", null, 12345, true);
        Document doc = baseDoc(List.of(f));
        doc.setTenant(tenant);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_TAX_2D_DOC,
                        DocumentRule.R_TAX_BAD_CLASSIFICATION,
                        DocumentRule.R_TAX_PARSE,
                        DocumentRule.R_TAX_FAKE,
                        DocumentRule.R_TAX_N1,
                        DocumentRule.R_TAX_N3,
                        DocumentRule.R_TAX_NAMES,
                        DocumentRule.R_TAX_LEAF,
                        DocumentRule.R_TAX_LEAF
                );
    }

    @Test
    @DisplayName("Process - HasBeenParsed inconclusive (bloquant) stoppe")
    void process_blocks_on_inconclusive_parse() {
        int year = LocalDate.now().getYear() - 1;
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").build();
        File f = taxFileNoParsed(year, "DUPONT JEAN", 12345);
        Document doc = baseDoc(List.of(f));
        doc.setTenant(tenant);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_TAX_2D_DOC,
                        DocumentRule.R_TAX_BAD_CLASSIFICATION
                );
        Assertions.assertThat(report.getInconclusiveRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_TAX_PARSE);
        Assertions.assertThat(report.getFailedRules()).isEmpty();
    }

    @Test
    @DisplayName("Process - Consistency failed (bloquant)")
    void process_blocks_on_consistency_failure() {
        int year = LocalDate.now().getYear() - 1;
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").build();
        // mismatch year between QR (year) and parsed (year-1) to déclencher failure
        ObjectNode qr = qrNode(year, "DUPONT JEAN", null, 12345);
        BarCodeFileAnalysis bar = BarCodeFileAnalysis.builder().barCodeType(BarCodeType.TWO_D_DOC).documentType(BarCodeDocumentType.TAX_ASSESSMENT).verifiedData(qr).build();
        TaxIncomeMainFile parsed = parsedMain(year - 1, "DUPONT JEAN", 12345, true); // year different
        ParsedFileAnalysis pfa = ParsedFileAnalysis.builder().analysisStatus(ParsedFileAnalysisStatus.COMPLETED).classification(ParsedFileClassification.TAX_INCOME).parsedFile(parsed).build();
        File f = File.builder().fileAnalysis(bar).parsedFileAnalysis(pfa).build(); bar.setFile(f); pfa.setFile(f);
        Document doc = baseDoc(List.of(f)); doc.setTenant(tenant);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_TAX_2D_DOC, DocumentRule.R_TAX_BAD_CLASSIFICATION, DocumentRule.R_TAX_PARSE);
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_TAX_FAKE);
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }

    @Test
    @DisplayName("Process - Echecs non bloquants (N1 & N3) n'arrêtent pas les autres")
    void process_non_blocking_failures_continue() {
        int year = LocalDate.now().getYear() - 5; // année très ancienne => N1 et N3 vont échouer
        Tenant tenant = Tenant.builder().firstName("JEAN").lastName("DUPONT").preferredName("MARTIN").build();
        File f = taxFile(year, "DUPONT JEAN", null, 12345, true);
        Document doc = baseDoc(List.of(f)); doc.setTenant(tenant);
        DocumentAnalysisReport report = emptyReport(doc);
        service.process(doc, report);
        Assertions.assertThat(report.getFailedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(DocumentRule.R_TAX_N1, DocumentRule.R_TAX_N3);
        Assertions.assertThat(report.getPassedRules()).extracting(DocumentAnalysisRule::getRule)
                .containsExactly(
                        DocumentRule.R_TAX_2D_DOC,
                        DocumentRule.R_TAX_BAD_CLASSIFICATION,
                        DocumentRule.R_TAX_PARSE,
                        DocumentRule.R_TAX_FAKE,
                        DocumentRule.R_TAX_NAMES,
                        DocumentRule.R_TAX_LEAF,
                        DocumentRule.R_TAX_LEAF
                );
        Assertions.assertThat(report.getInconclusiveRules()).isEmpty();
    }
}

