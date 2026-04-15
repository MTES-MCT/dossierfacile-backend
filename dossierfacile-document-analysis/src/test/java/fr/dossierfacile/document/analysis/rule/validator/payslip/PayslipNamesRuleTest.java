package fr.dossierfacile.document.analysis.rule.validator.payslip;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentIAFileAnalysis;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.entity.rule.PayslipNamesRuleData;
import fr.dossierfacile.common.enums.DocumentIAFileAnalysisStatus;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.common.model.document_ia.ExtractionModel;
import fr.dossierfacile.common.model.document_ia.GenericProperty;
import fr.dossierfacile.common.model.document_ia.ResultModel;
import fr.dossierfacile.document.analysis.rule.validator.RuleValidatorOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PayslipNamesRuleTest {

    private final PayslipNamesRule rule = new PayslipNamesRule();

    // ==========================
    // Helpers / Fixtures
    // ==========================

    private DocumentIAFileAnalysis analysisWithExtraction(String identiteSalarie) {
        List<GenericProperty> props = new ArrayList<>();
        if (identiteSalarie != null) {
            props.add(GenericProperty.builder().name("identite_salarie").type("string").value(identiteSalarie).build());
        }
        ExtractionModel extraction = ExtractionModel.builder().type("bulletin_salaire").properties(props).build();
        ResultModel result = ResultModel.builder().extraction(extraction).barcodes(List.of()).build();
        return DocumentIAFileAnalysis.builder().analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS).result(result).build();
    }

    private DocumentIAFileAnalysis analysisWith2DDoc(String ligne1, String nom, String prenom) {
        List<GenericProperty> beneficiaireProps = Stream.of(
                ligne1  != null ? GenericProperty.builder().name("ligne1").type("string").value(ligne1).build() : null,
                nom     != null ? GenericProperty.builder().name("nom").type("string").value(nom).build()       : null,
                prenom  != null ? GenericProperty.builder().name("prenom").type("string").value(prenom).build() : null
        ).filter(Objects::nonNull).toList();

        GenericProperty beneficiaire = GenericProperty.builder()
                .name("beneficiaire")
                .type("object")
                .value(beneficiaireProps)
                .build();

        BarcodeModel barcode = BarcodeModel.builder()
                .type("2D_DOC")
                .typedData(List.of(beneficiaire))
                .build();

        ResultModel result = ResultModel.builder().extraction(null).barcodes(List.of(barcode)).build();
        return DocumentIAFileAnalysis.builder().analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS).result(result).build();
    }

    private DocumentIAFileAnalysis analysisWith2DDocAndExtraction(String ligne1, String identiteSalarie) {
        List<GenericProperty> beneficiaireProps = List.of(
                GenericProperty.builder().name("ligne1").type("string").value(ligne1).build()
        );
        GenericProperty beneficiaire = GenericProperty.builder()
                .name("beneficiaire").type("object").value(beneficiaireProps).build();
        BarcodeModel barcode = BarcodeModel.builder().type("2D_DOC").typedData(List.of(beneficiaire)).build();

        ExtractionModel extraction = ExtractionModel.builder()
                .type("bulletin_salaire")
                .properties(List.of(GenericProperty.builder().name("identite_salarie").type("string").value(identiteSalarie).build()))
                .build();

        ResultModel result = ResultModel.builder().extraction(extraction).barcodes(List.of(barcode)).build();
        return DocumentIAFileAnalysis.builder().analysisStatus(DocumentIAFileAnalysisStatus.SUCCESS).result(result).build();
    }

    private DocumentIAFileAnalysis failedAnalysis() {
        return DocumentIAFileAnalysis.builder().analysisStatus(DocumentIAFileAnalysisStatus.FAILED).build();
    }

    private Document documentWithTenant(String firstName, String lastName, DocumentIAFileAnalysis... analyses) {
        Tenant tenant = Tenant.builder().firstName(firstName).lastName(lastName).build();
        return buildDocument(tenant, null, analyses);
    }

    private Document documentWithGuarantor(String firstName, String lastName, DocumentIAFileAnalysis... analyses) {
        Guarantor guarantor = Guarantor.builder().firstName(firstName).lastName(lastName).build();
        return buildDocument(null, guarantor, analyses);
    }

    private Document buildDocument(Tenant tenant, Guarantor guarantor, DocumentIAFileAnalysis... analyses) {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < analyses.length; i++) {
            DocumentIAFileAnalysis analysis = analyses[i];
            File file = File.builder()
                    .id((long) (i + 1))
                    .documentIAFileAnalysis(analysis)
                    .storageFile(StorageFile.builder().name("file-" + (i + 1) + ".pdf").build())
                    .build();
            if (analysis != null) {
                analysis.setFile(file);
            }
            files.add(file);
        }

        Document doc = Document.builder().files(files).tenant(tenant).guarantor(guarantor).build();
        files.forEach(f -> f.setDocument(doc));
        return doc;
    }

    // ==========================
    // Tests — INCONCLUSIVE
    // ==========================

    @Test
    @DisplayName("INCONCLUSIVE si aucune analyse disponible")
    void inconclusive_when_no_analysis() {
        Document doc = Document.builder().files(List.of())
                .tenant(Tenant.builder().firstName("Jean").lastName("Dupont").build())
                .build();

        RuleValidatorOutput out = rule.validate(doc);

        assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
        assertThat(out.rule().getRule()).isEqualTo(DocumentRule.R_PAYSLIP_NAME_MATCH);
    }

    @Test
    @DisplayName("INCONCLUSIVE si aucun tenant ni garant")
    void inconclusive_when_no_tenant_no_guarantor() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("DUPONT JEAN");
        Document doc = buildDocument(null, null, analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("INCONCLUSIVE si une analyse est en échec")
    void inconclusive_when_any_analysis_failed() {
        DocumentIAFileAnalysis success = analysisWithExtraction("DUPONT JEAN");
        DocumentIAFileAnalysis failed = failedAnalysis();

        Document doc = documentWithTenant("Jean", "Dupont", success, failed);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.INCONCLUSIVE);
    }

    @Test
    @DisplayName("FAILED si identite_salarie est absent de l'extraction et pas de 2DDoc")
    void failed_when_identity_null_in_extraction_and_no_2ddoc() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction(null);
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        RuleValidatorOutput output = rule.validate(doc);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRuleData()).isInstanceOf(PayslipNamesRuleData.class);
        PayslipNamesRuleData data = (PayslipNamesRuleData) output.rule().getRuleData();
        assertThat(data.payslipNamesEntryList()).hasSize(1);
        assertThat(data.payslipNamesEntryList().get(0).fileId()).isEqualTo(1L);
        assertThat(data.payslipNamesEntryList().get(0).fileName()).isEqualTo("file-1.pdf");
        assertThat(data.payslipNamesEntryList().get(0).ExtractedName()).isNull();
    }

    @Test
    @DisplayName("PASSED et RuleData vide quand tous les bulletins matchent")
    void passed_with_empty_rule_data_when_all_names_match() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR DUPONT JEAN");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        RuleValidatorOutput out = rule.validate(doc);

        assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
        assertThat(out.rule().getRuleData()).isInstanceOf(PayslipNamesRuleData.class);
        PayslipNamesRuleData data = (PayslipNamesRuleData) out.rule().getRuleData();
        assertThat(data.payslipNamesEntryList()).isEmpty();
    }

    // ==========================
    // Tests — PASSED via extraction
    // ==========================

    @Test
    @DisplayName("PASSED avec identite_salarie contenant nom et prénom (extraction seule)")
    void passed_with_extraction_identity() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR DUPONT JEAN");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        RuleValidatorOutput out = rule.validate(doc);

        assertThat(out.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec extraction et tenant garant")
    void passed_with_extraction_and_guarantor() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR MARTIN PAUL");
        Document doc = documentWithGuarantor("Paul", "Martin", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    // ==========================
    // Tests — PASSED via 2DDOC beneficiaire.ligne1
    // ==========================

    @Test
    @DisplayName("PASSED avec 2DDOC beneficiaire.ligne1")
    void passed_with_2ddoc_beneficiaire_ligne1() {
        DocumentIAFileAnalysis analysis = analysisWith2DDoc("MR DUPONT JEAN", null, null);
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    // ==========================
    // Tests — PASSED via 2DDOC beneficiaire.nom + prenom
    // ==========================

    @Test
    @DisplayName("PASSED avec 2DDOC beneficiaire.nom + beneficiaire.prenom (ligne1 absent)")
    void passed_with_2ddoc_beneficiaire_nom_and_prenom() {
        DocumentIAFileAnalysis analysis = analysisWith2DDoc(null, "DUPONT", "JEAN");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("FAILED avec 2DDOC beneficiaire.nom seul (ligne1 et prenom absents) — le prénom est introuvable dans la chaîne")
    void failed_with_2ddoc_beneficiaire_nom_only() {
        DocumentIAFileAnalysis analysis = analysisWith2DDoc(null, "DUPONT", null);
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        // "Jean" n'est pas trouvé dans "DUPONT" → hasFirstNameMatch échoue → FAILED
        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    // ==========================
    // Tests — priorité 2DDOC sur extraction
    // ==========================

    @Test
    @DisplayName("2DDOC beneficiaire.ligne1 a priorité sur identite_salarie de l'extraction")
    void twoDDoc_takes_priority_over_extraction() {
        // 2DDOC dit "DUPONT JEAN" (correct), extraction dit "AUTRE PERSONNE" (incorrect si utilisée)
        DocumentIAFileAnalysis analysis = analysisWith2DDocAndExtraction("MR DUPONT JEAN", "AUTRE PERSONNE");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("Fallback sur extraction quand aucun champ beneficiaire n'est renseigné dans le 2DDOC")
    void fallback_to_extraction_when_2ddoc_beneficiaire_empty() {
        // beneficiaire sans aucun champ renseigné
        DocumentIAFileAnalysis analysis = analysisWith2DDocAndExtraction(null, "MR DUPONT JEAN");
        // Dans ce cas, ligne1=null et tous les champs beneficiaire sont null →
        // resolveIdentityString() retourne null → getIdentityString() retourne identiteString
        // Mais ici on passe ligne1=null ce qui construit un barcode avec un beneficiaire vide.
        // Le mapper ne trouvera rien de non-null dans BeneficiaireModel → fallback extraction.
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    // ==========================
    // Tests — fuzzy matching
    // ==========================

    @Test
    @DisplayName("PASSED avec accents normalisés dans l'identité extraite")
    void passed_with_accents_normalized() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR MARTIN-BERNARD THEO");
        Document doc = documentWithTenant("Théo", "Martin-Bernard", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec distance de Levenshtein ≤ 2 sur le nom")
    void passed_with_levenshtein_distance_on_last_name() {
        // "DUPOND" vs "DUPONT" → distance 1
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR DUPOND JEAN");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("PASSED avec nom composé et tiret dans l'extraction")
    void passed_with_hyphenated_name_in_extraction() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR MARTIN BERNARD THEO");
        Document doc = documentWithTenant("Théo", "Martin-Bernard", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    // ==========================
    // Tests — FAILED
    // ==========================

    @Test
    @DisplayName("FAILED si le nom dans l'extraction ne correspond pas")
    void failed_when_last_name_does_not_match() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR MARTIN JEAN");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        RuleValidatorOutput output = rule.validate(doc);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRuleData()).isInstanceOf(PayslipNamesRuleData.class);
        PayslipNamesRuleData data = (PayslipNamesRuleData) output.rule().getRuleData();
        assertThat(data.payslipNamesEntryList()).hasSize(1);
        assertThat(data.payslipNamesEntryList().get(0).fileId()).isEqualTo(1L);
        assertThat(data.payslipNamesEntryList().get(0).fileName()).isEqualTo("file-1.pdf");
        assertThat(data.payslipNamesEntryList().get(0).ExtractedName()).isEqualTo("MR MARTIN JEAN");

        PayslipNamesRuleData.Name expected = data.expectedName();
        assertThat(expected.firstNames()).isEqualTo("Jean");
        assertThat(expected.lastName()).isEqualTo("Dupont");
    }

    @Test
    @DisplayName("FAILED si le prénom dans l'extraction ne correspond pas")
    void failed_when_first_name_does_not_match() {
        DocumentIAFileAnalysis analysis = analysisWithExtraction("MR DUPONT PIERRE");
        Document doc = documentWithTenant("Jean", "Dupont", analysis);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
    }

    // ==========================
    // Tests — multi-bulletins
    // ==========================

    @Test
    @DisplayName("PASSED si tous les bulletins correspondent au même tenant")
    void passed_when_all_payslips_match() {
        DocumentIAFileAnalysis m1 = analysisWithExtraction("MR DUPONT JEAN");
        DocumentIAFileAnalysis m2 = analysisWithExtraction("MR DUPONT JEAN");
        DocumentIAFileAnalysis m3 = analysisWith2DDoc("MR DUPONT JEAN", null, null);

        Document doc = documentWithTenant("Jean", "Dupont", m1, m2, m3);

        assertThat(rule.validate(doc).ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.PASSED);
    }

    @Test
    @DisplayName("FAILED si un des bulletins ne correspond pas")
    void failed_when_one_payslip_does_not_match() {
        DocumentIAFileAnalysis match    = analysisWithExtraction("MR DUPONT JEAN");
        DocumentIAFileAnalysis mismatch = analysisWithExtraction("MR MARTIN PIERRE");

        Document doc = documentWithTenant("Jean", "Dupont", match, mismatch);

        RuleValidatorOutput output = rule.validate(doc);

        assertThat(output.ruleLevel()).isEqualTo(RuleValidatorOutput.RuleLevel.FAILED);
        assertThat(output.rule().getRuleData()).isInstanceOf(PayslipNamesRuleData.class);
        PayslipNamesRuleData data = (PayslipNamesRuleData) output.rule().getRuleData();
        assertThat(data.payslipNamesEntryList()).hasSize(1);
        assertThat(data.payslipNamesEntryList().get(0).fileId()).isEqualTo(2L);
        assertThat(data.payslipNamesEntryList().get(0).fileName()).isEqualTo("file-2.pdf");
        assertThat(data.payslipNamesEntryList().get(0).ExtractedName()).isEqualTo("MR MARTIN PIERRE");

    }
}
