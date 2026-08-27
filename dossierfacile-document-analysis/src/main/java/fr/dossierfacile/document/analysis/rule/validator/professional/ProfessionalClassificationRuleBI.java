package fr.dossierfacile.document.analysis.rule.validator.professional;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentRule;
import fr.dossierfacile.common.model.document_ia.BarcodeModel;
import fr.dossierfacile.document.analysis.rule.validator.document_ia.BaseDocumentIAValidator;

/*
 * Rule R_DOCUMENT_IA_CLASSIFICATION (Professional classification variant BI):
 *
 * Cette règle vérifie la classification du document professionnel via 2D-Doc.
 *
 * Données examinées:
 * - Les barcodes du document issus des analyses Document-IA réussies.
 * - Le type de 2D-Doc (`doc_type`).
 *
 * Fonctionnement:
 * - Vérifie qu'au moins un barcode 2D-Doc possède un `doc_type` égal à "29" (Attestation d'activité professionnelle).
 * - Retourne PASSED si un tel 2D-Doc est présent.
 * - Retourne INCONCLUSIVE et BLOQUANT sinon (générant un statut d'analyse UNDEFINED).
 */
public class ProfessionalClassificationRuleBI extends BaseDocumentIAValidator {

    private static final String EXPECTED_DOC_TYPE = "29";

    @Override
    protected boolean isBlocking() {
        return true;
    }

    @Override
    protected boolean isInconclusive() {
        return true;
    }

    @Override
    protected DocumentRule getRule() {
        return DocumentRule.R_DOCUMENT_IA_CLASSIFICATION;
    }

    @Override
    protected boolean isValid(Document document) {
        var documentIAAnalyses = this.getSuccessfulDocumentIAAnalyses(document);
        if (documentIAAnalyses.isEmpty()) {
            return false;
        }

        for (var analysis : documentIAAnalyses) {
            if (analysis.getResult() != null && analysis.getResult().getBarcodes() != null) {
                for (BarcodeModel barcode : analysis.getResult().getBarcodes()) {
                    if (isType29Barcode(barcode)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isType29Barcode(BarcodeModel barcode) {
        if (barcode == null) {
            return false;
        }
        return EXPECTED_DOC_TYPE.equals(barcode.getDocType());
    }
}
